package com.sor999.dalmoi.support;

import com.sor999.dalmoi.course.domain.Course;
import com.sor999.dalmoi.crew.domain.Crew;
import com.sor999.dalmoi.crew.domain.CrewMember;
import com.sor999.dalmoi.fixture.MemberFixture;
import com.sor999.dalmoi.member.domain.Member;
import com.sor999.dalmoi.member.repository.MemberRepository;
import com.sor999.dalmoi.auth.service.TokenService;
import com.sor999.dalmoi.record.domain.RecordJob;
import com.sor999.dalmoi.session.domain.RunningSession;
import com.sor999.dalmoi.session.domain.SessionParticipant;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.sql.Timestamp;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class MvpDatabaseTest {

    @Autowired
    protected EntityManager entityManager;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected MemberRepository memberRepository;

    @Autowired
    protected TokenService tokenService;

    @Autowired
    protected Clock clock;

    protected record Scenario(Long memberId, Long crewId, Long courseId, Long sessionId, Long participantId,
            String inviteCode) {
    }

    protected Long member() {
        return memberRepository.saveAndFlush(MemberFixture.createMember()).getId();
    }

    protected Scenario waiting() {
        return transactionTemplate.execute(tx -> {
            Member owner = MemberFixture.createMember();
            entityManager.persist(owner);
            String invite = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
            Crew crew = Crew.create(owner, "달모이", invite);
            entityManager.persist(crew);
            entityManager.persist(CrewMember.create(crew, owner));
            GeometryFactory geometry = new GeometryFactory(new PrecisionModel(), 4326);
            Course course = Course.create(owner, "강변 코스", geometry.createLineString(new Coordinate[]{
                    new Coordinate(126.93, 37.52), new Coordinate(126.931, 37.521)}), 140, false);
            entityManager.persist(course);
            RunningSession session = RunningSession.create(crew, owner, course, "저녁 러닝");
            entityManager.persist(session);
            SessionParticipant participant = SessionParticipant.create(session, owner);
            entityManager.persist(participant);
            entityManager.flush();
            return new Scenario(owner.getId(), crew.getId(), course.getId(), session.getId(), participant.getId(),
                    invite);
        });
    }

    protected Scenario running() {
        Scenario s = waiting();
        jdbcTemplate.update("update running_sessions set status='RUNNING',started_at=? where id=?",
                Timestamp.from(clock.instant().minusSeconds(20)), s.sessionId());
        jdbcTemplate.update("update session_participants set status='RUNNING',started_at=? where id=?",
                Timestamp.from(clock.instant().minusSeconds(10)), s.participantId());
        return s;
    }

    protected Long participant(Scenario s, Long memberId, boolean running) {
        Long id = transactionTemplate.execute(tx -> {
            Member member = entityManager.getReference(Member.class, memberId);
            Crew crew = entityManager.getReference(Crew.class, s.crewId());
            entityManager.persist(CrewMember.create(crew, member));
            SessionParticipant participant = SessionParticipant.create(
                    entityManager.getReference(RunningSession.class, s.sessionId()), member);
            entityManager.persist(participant);
            entityManager.flush();
            return participant.getId();
        });
        if (running) {
            jdbcTemplate.update("update session_participants set status='RUNNING',started_at=? where id=?",
                    Timestamp.from(clock.instant().minusSeconds(10)), id);
        }
        return id;
    }

    protected UUID storedGps(Scenario s, long sequence) {
        UUID event = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into gps_events(event_id,participant_id,sequence,ingest_order,location,accuracy_m,
                    recorded_at,received_at,validation_status,processed_at)
                values(?,?,?,?,ST_SetSRID(ST_MakePoint(126.93,37.52),4326),5,now(),now(),'VALID',now())
                """, event, s.participantId(), sequence, sequence + 1);
        return event;
    }

    protected void enqueueRecord(Scenario s) {
        jdbcTemplate.update("update session_participants set status='FINISHED',ended_at=? where id=?",
                Timestamp.from(clock.instant()), s.participantId());
        long cutoff = jdbcTemplate.queryForObject(
                "select coalesce(max(ingest_order),0) from gps_events where participant_id=?",
                Long.class, s.participantId());
        transactionTemplate.executeWithoutResult(tx -> {
            entityManager.persist(
                    RecordJob.create(entityManager.getReference(SessionParticipant.class, s.participantId()), cutoff));
            entityManager.flush();
        });
    }
}
