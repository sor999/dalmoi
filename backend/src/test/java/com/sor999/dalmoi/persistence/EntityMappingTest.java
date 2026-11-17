package com.sor999.dalmoi.persistence;

import com.sor999.dalmoi.auth.domain.AuthSession;
import com.sor999.dalmoi.course.domain.Course;
import com.sor999.dalmoi.crew.domain.Crew;
import com.sor999.dalmoi.crew.domain.CrewMember;
import com.sor999.dalmoi.fixture.MemberFixture;
import com.sor999.dalmoi.gps.domain.GpsEvent;
import com.sor999.dalmoi.gps.domain.GpsValidationStatus;
import com.sor999.dalmoi.member.domain.Member;
import com.sor999.dalmoi.member.domain.MemberCredential;
import com.sor999.dalmoi.record.domain.RecordJob;
import com.sor999.dalmoi.record.domain.RecordJobStatus;
import com.sor999.dalmoi.record.domain.RunningRecord;
import com.sor999.dalmoi.session.domain.ParticipantStatus;
import com.sor999.dalmoi.session.domain.RunningSession;
import com.sor999.dalmoi.session.domain.SessionParticipant;
import com.sor999.dalmoi.session.domain.SessionStatus;
import com.sor999.dalmoi.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PostgresIntegrationTest
@Transactional
class EntityMappingTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("엔티티와 공간 좌표 및 공유 기본키를 저장하고 다시 조회한다")
    void persist_allMvpEntities_reloadsMappedValues() {
        Member member = persistMember();
        MemberCredential credential = persist(MemberCredential.create(member, "password-hash"));
        Instant issuedAt = Instant.parse("2026-11-16T00:00:00Z");
        AuthSession authSession = persist(
                AuthSession.create(member, "refresh-hash", issuedAt, issuedAt.plusSeconds(3600)));
        Course course = persist(Course.create(member, "한강 코스", route(50), 1200.5, false));
        Crew crew = persist(Crew.create(member, "달모이", "INVITE"));
        CrewMember crewMember = persist(CrewMember.create(crew, member));
        RunningSession session = persist(RunningSession.create(crew, member, course, "아침 러닝"));
        SessionParticipant participant = persist(SessionParticipant.create(session, member));
        UUID eventId = UUID.randomUUID();
        GpsEvent event = persist(GpsEvent.create(eventId, participant, 0, 1,
                GEOMETRY_FACTORY.createPoint(new Coordinate(126.93, 37.52)), 8.5, issuedAt, issuedAt));

        entityManager.flush();

        jdbcTemplate.update(
                "UPDATE session_participants SET status = 'FINISHED', started_at = ?, ended_at = ? WHERE id = ?",
                java.sql.Timestamp.from(issuedAt), java.sql.Timestamp.from(issuedAt.plusSeconds(60)),
                participant.getId());

        RecordJob job = persist(RecordJob.create(participant, 1));
        RunningRecord record = persist(RunningRecord.create(participant, 100.5, 60, "v1"));

        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Member.class, member.getId()).getProfileImageKey()).isNull();

        MemberCredential savedCredential = entityManager.find(MemberCredential.class, member.getId());
        assertThat(savedCredential.getId()).isEqualTo(credential.getId()).isEqualTo(member.getId());
        assertThat(savedCredential.getPasswordHash()).isEqualTo("password-hash");
        assertThat(entityManager.find(AuthSession.class, authSession.getId()).getExpiresAt())
                .isEqualTo(issuedAt.plusSeconds(3600));

        Course savedCourse = entityManager.find(Course.class, course.getId());
        assertThat(savedCourse.getRoute().getSRID()).isEqualTo(4326);
        assertThat(savedCourse.getRoute().getCoordinates()).containsExactly(route(50).getCoordinates());
        assertThat(savedCourse.getEstimatedDistanceMeters()).isEqualTo(1200.5);
        assertThat(entityManager.find(Crew.class, crew.getId()).getMaxMembers()).isEqualTo((short) 10);
        assertThat(entityManager.find(CrewMember.class, crewMember.getId()).getMember().getId())
                .isEqualTo(member.getId());

        RunningSession savedSession = entityManager.find(RunningSession.class, session.getId());
        assertThat(savedSession.getStatus()).isEqualTo(SessionStatus.WAITING);
        assertThat(savedSession.getPlannedRoute().getCoordinates()).containsExactly(route(50).getCoordinates());
        assertThat(savedSession.getCrewNameSnapshot()).isEqualTo("달모이");
        assertThat(entityManager.find(SessionParticipant.class, participant.getId()).getStatus())
                .isEqualTo(ParticipantStatus.FINISHED);

        GpsEvent savedEvent = entityManager.find(GpsEvent.class, eventId);
        assertThat(savedEvent.getValidationStatus()).isEqualTo(GpsValidationStatus.PENDING);
        assertThat(savedEvent.getLocation().getX()).isEqualTo(126.93);
        assertThat(savedEvent.getLocation().getY()).isEqualTo(37.52);

        RecordJob savedJob = entityManager.find(RecordJob.class, participant.getId());
        assertThat(savedJob.getId()).isEqualTo(job.getId()).isEqualTo(participant.getId());
        assertThat(savedJob.getStatus()).isEqualTo(RecordJobStatus.PROCESSING);
        assertThat(savedJob.getCutoffIngestOrder()).isEqualTo(1);
        assertThat(entityManager.find(RunningRecord.class, record.getId()).getDurationSeconds()).isEqualTo(60);
    }

    @ParameterizedTest(name = "{0}개 좌표")
    @ValueSource(ints = {2, 50})
    @DisplayName("코스의 최소 및 최대 좌표 수를 저장하고 순서를 복원한다")
    void persist_boundaryCoursePointCounts_preservesCoordinateOrder(int pointCount) {
        Course course = persist(Course.create(persistMember(), "코스", route(pointCount), 0, false));

        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Course.class, course.getId()).getRoute().getCoordinates())
                .containsExactly(route(pointCount).getCoordinates());
    }

    @ParameterizedTest(name = "{0}개 좌표")
    @ValueSource(ints = {1, 51})
    @DisplayName("DB는 허용 범위 밖의 코스 좌표 수를 거부한다")
    void updateRoute_invalidPointCount_throwsException(int pointCount) {
        Course course = persist(Course.create(persistMember(), "코스", route(2), 0, false));

        entityManager.flush();

        String wkt = pointCount == 1 ? "LINESTRING(126.93 37.52)" : route(pointCount).toText();

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE courses SET route = ST_GeomFromText(?, 4326) WHERE id = ?",
                wkt, course.getId())).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("DB는 위경도 범위를 벗어난 코스 좌표를 거부한다")
    void updateRoute_outOfRangeCoordinates_throwsException() {
        Course course = persist(Course.create(persistMember(), "코스", route(2), 0, false));

        entityManager.flush();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE courses SET route = ST_GeomFromText('LINESTRING(126 37,181 38)',4326) WHERE id = ?",
                course.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("동일 회원의 활성 크루 가입 중복은 거부한다")
    void insertCrewMember_duplicateActiveMembership_throwsException() {
        Member member = persistMember();
        Crew crew = persist(Crew.create(member, "달모이", "INVITE"));
        persist(CrewMember.create(crew, member));

        entityManager.flush();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO crew_members(crew_id, member_id, joined_at) VALUES (?, ?, now())", crew.getId(),
                member.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 참가자의 GPS 순번 중복을 거부한다")
    void insertGps_duplicateSequence_throwsException() {
        SessionParticipant participant = participant();

        entityManager.flush();

        insertGps(participant.getId(), UUID.randomUUID(), 0, 1, 8.5);

        assertThatThrownBy(() -> insertGps(participant.getId(), UUID.randomUUID(), 0, 2, 8.5))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "정확도 {0}")
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY})
    @DisplayName("GPS 정확도는 유한한 0 이상의 값만 허용한다")
    void insertGps_invalidAccuracy_throwsException(double accuracy) {
        SessionParticipant participant = participant();

        entityManager.flush();

        assertThatThrownBy(() -> insertGps(participant.getId(), UUID.randomUUID(), 0, 1, accuracy))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("RUNNING 상태에는 개인 시작 시각이 반드시 있어야 한다")
    void updateParticipant_runningWithoutStartTime_throwsException() {
        SessionParticipant participant = participant();

        entityManager.flush();

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE session_participants SET status = 'RUNNING' WHERE id = ?",
                participant.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("한 회원이 서로 다른 세션에서 동시에 주행할 수 없다")
    void updateParticipant_multipleRunningSessionsForMember_throwsException() {
        Member member = persistMember();
        Course course = persist(Course.create(member, "코스", route(2), 0, false));
        Crew crew = persist(Crew.create(member, "달모이", "INVITE"));
        RunningSession first = persist(RunningSession.create(crew, member, course, "첫 러닝"));
        RunningSession second = persist(RunningSession.create(crew, member, course, "둘째 러닝"));
        SessionParticipant firstParticipant = persist(SessionParticipant.create(first, member));
        SessionParticipant secondParticipant = persist(SessionParticipant.create(second, member));

        entityManager.flush();

        jdbcTemplate.update("UPDATE session_participants SET status = 'RUNNING', started_at = now() WHERE id = ?",
                firstParticipant.getId());

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE session_participants SET status = 'RUNNING', started_at = now() WHERE id = ?",
                secondParticipant.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("한 크루에서 두 그룹 러닝을 동시에 진행할 수 없다")
    void updateSession_multipleRunningSessionsForCrew_throwsException() {
        Member member = persistMember();
        Course course = persist(Course.create(member, "코스", route(2), 0, false));
        Crew crew = persist(Crew.create(member, "달모이", "INVITE"));
        RunningSession first = persist(RunningSession.create(crew, member, course, "첫 러닝"));
        RunningSession second = persist(RunningSession.create(crew, member, course, "둘째 러닝"));

        entityManager.flush();

        jdbcTemplate.update("UPDATE running_sessions SET status = 'RUNNING', started_at = now() WHERE id = ?",
                first.getId());

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE running_sessions SET status = 'RUNNING', started_at = now() WHERE id = ?", second.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("원본 코스 변경으로 기존 세션의 계획 경로가 바뀌지 않는다")
    void updateCourse_existingSession_preservesSnapshot() {
        Member member = persistMember();
        Course course = persist(Course.create(member, "원본", route(2), 100, false));
        Crew crew = persist(Crew.create(member, "달모이", "INVITE"));
        RunningSession session = persist(RunningSession.create(crew, member, course, "러닝"));

        entityManager.flush();

        jdbcTemplate.update(
                "UPDATE courses SET title = '변경', route = ST_GeomFromText(?,4326), estimated_distance_m = 200, deleted_at = now() WHERE id = ?",
                route(3).toText(), course.getId());
        entityManager.clear();

        RunningSession saved = entityManager.find(RunningSession.class, session.getId());

        assertThat(saved.getPlannedCourseTitle()).isEqualTo("원본");
        assertThat(saved.getPlannedDistanceMeters()).isEqualTo(100);
        assertThat(saved.getPlannedRoute().getCoordinates()).containsExactly(route(2).getCoordinates());
    }

    private Member persistMember() {
        return persist(MemberFixture.createMember());
    }

    private SessionParticipant participant() {
        Member member = persistMember();
        Course course = persist(Course.create(member, "코스", route(2), 0, false));
        Crew crew = persist(Crew.create(member, "달모이", "INVITE"));
        RunningSession session = persist(RunningSession.create(crew, member, course, "러닝"));

        return persist(SessionParticipant.create(session, member));
    }

    private void insertGps(Long participantId, UUID eventId, long sequence, long ingestOrder, double accuracy) {
        jdbcTemplate.update("""
                INSERT INTO gps_events(event_id, participant_id, sequence, ingest_order, location,
                    accuracy_m, recorded_at, received_at, validation_status)
                VALUES (?, ?, ?, ?, ST_SetSRID(ST_MakePoint(126.93,37.52),4326), ?, now(), now(), 'PENDING')
                """, eventId, participantId, sequence, ingestOrder, accuracy);
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    private static LineString route(int pointCount) {
        Coordinate[] coordinates = new Coordinate[pointCount];

        for (int i = 0; i < pointCount; i++) {
            coordinates[i] = new Coordinate(126.93 + i * 0.0001, 37.52 + i * 0.0001);
        }

        return GEOMETRY_FACTORY.createLineString(coordinates);
    }
}
