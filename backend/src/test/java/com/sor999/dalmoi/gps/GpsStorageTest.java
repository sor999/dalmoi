package com.sor999.dalmoi.gps;

import com.sor999.dalmoi.gps.dto.GpsRequest;
import com.sor999.dalmoi.gps.domain.GpsEvent;
import com.sor999.dalmoi.gps.domain.GpsValidationStatus;
import com.sor999.dalmoi.gps.repository.GpsEventRepository;
import com.sor999.dalmoi.gps.service.GpsService;
import com.sor999.dalmoi.session.domain.SessionParticipant;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.support.MvpDatabaseTest;
import com.sor999.dalmoi.support.PostgresIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import com.sor999.dalmoi.course.service.GeoSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PostgresIntegrationTest
class GpsStorageTest extends MvpDatabaseTest {

    @Autowired
    private GpsService gpsService;

    @Autowired
    private GpsEventRepository gpsEventRepository;

    @Test
    @DisplayName("GPS 저장과 동일 이벤트 재전송은 멱등 처리하고 내용 충돌은 거절한다")
    void receive_repeatedEvent_preservesSingleHistory() {
        Scenario s = running();
        GpsRequest request = request(0, 37.52, clock.instant());
        assertThat(gpsService.receive(s.memberId(), s.sessionId(), request).getValidationStatus())
                .isEqualTo(GpsValidationStatus.VALID);
        gpsService.receive(s.memberId(), s.sessionId(), request);
        assertThat(gpsEventRepository.findByParticipantIdOrderBySequenceAsc(s.participantId())).hasSize(1);
        assertThatThrownBy(() -> gpsService.receive(s.memberId(), s.sessionId(),
                new GpsRequest(request.eventId(), 0L, 37.53, 126.93, 5.0, request.recordedAt())))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> gpsService.receive(s.memberId(), s.sessionId(), request(0, 37.52, clock.instant())))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("역순과 불량 GPS를 이력으로 남기고 허용 시각 위반 및 종료 후 신규 GPS는 거절한다")
    void receive_invalidQualityAndTime_recordsRejectionAndStopsAfterFinish() {
        Scenario s = running();
        Instant now = clock.instant();
        gpsService.receive(s.memberId(), s.sessionId(), request(2, 37.52, now.minusSeconds(5)));
        assertThat(gpsService.receive(s.memberId(), s.sessionId(), request(1, 37.52, now.minusSeconds(6)))
                .getRejectionReason()).isEqualTo("LATE_SEQUENCE");
        assertThat(gpsService
                .receive(s.memberId(), s.sessionId(), new GpsRequest(UUID.randomUUID(), 3L, 37.52, 126.93, 51.0, now))
                .getRejectionReason()).isEqualTo("LOW_ACCURACY");
        assertThat(gpsService.receive(s.memberId(), s.sessionId(), request(4, 38.52, now)).getRejectionReason())
                .isEqualTo("UNREALISTIC_SPEED");
        assertThatThrownBy(
                () -> gpsService.receive(s.memberId(), s.sessionId(), request(5, 37.52, now.plusSeconds(11))))
                .isInstanceOf(BusinessException.class);
        jdbcTemplate.update("update session_participants set status='FINISHED',ended_at=now() where id=?",
                s.participantId());
        assertThatThrownBy(() -> gpsService.receive(s.memberId(), s.sessionId(), request(6, 37.52, now)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("새 GPS의 UUID 충돌이 기존 이력을 merge로 덮어쓰지 않는다")
    void save_existingAssignedId_rejectsInsteadOfOverwriting() {
        Scenario s = running();
        GpsEvent first = gpsService.receive(s.memberId(), s.sessionId(), request(0, 37.52, clock.instant()));
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(tx -> {
            GpsEvent duplicate = GpsEvent.create(first.getEventId(),
                    entityManager.getReference(SessionParticipant.class, s.participantId()),
                    1, 2, GeoSupport.FACTORY.createPoint(new Coordinate(126.94, 37.53)), 5, clock.instant(),
                    clock.instant());
            duplicate.validate(clock.instant());
            gpsEventRepository.saveAndFlush(duplicate);
        })).isInstanceOf(DataIntegrityViolationException.class);
    }

    private GpsRequest request(long sequence, double latitude, Instant recordedAt) {
        return new GpsRequest(UUID.randomUUID(), sequence, latitude, 126.93, 5.0, recordedAt);
    }
}
