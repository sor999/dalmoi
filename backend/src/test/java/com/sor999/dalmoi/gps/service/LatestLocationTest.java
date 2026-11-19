package com.sor999.dalmoi.gps.service;

import com.sor999.dalmoi.gps.dto.LocationResponse;
import com.sor999.dalmoi.gps.event.GpsStored;
import com.sor999.dalmoi.session.event.SessionChanged;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.support.MvpDatabaseTest;
import com.sor999.dalmoi.support.MvpIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MvpIntegrationTest
class LatestLocationTest extends MvpDatabaseTest {

    @Autowired
    private LocationService locationService;

    @Autowired
    private LocationCache locationCache;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("커밋 후 GPS 이벤트를 캐시하고 종료 이벤트에서 캐시를 제거한다")
    void committedEvents_updatesCacheAndRemovesFinishedParticipant() {
        Scenario s = running();
        UUID id = storedGps(s, 0);
        Instant now = clock.instant();
        LocationResponse.Location location = new LocationResponse.Location(id, s.memberId(), 0, 37.52, 126.93, 5,
                now, now, now, false);
        transactionTemplate.executeWithoutResult(
                tx -> eventPublisher.publishEvent(new GpsStored(s.sessionId(), s.participantId(), location)));
        assertThat(locationCache.latest(s.sessionId(), s.participantId(), () -> {
            throw new AssertionError("캐시 적중 필요");
        }).eventId()).isEqualTo(id);
        assertThat(locationService.latest(s.memberId(), s.sessionId()).locations()).hasSize(1);
        jdbcTemplate.update("update session_participants set status='FINISHED',ended_at=now() where id=?",
                s.participantId());
        transactionTemplate.executeWithoutResult(tx -> eventPublisher.publishEvent(
                new SessionChanged(s.sessionId(), "PARTICIPANT_FINISHED", s.memberId(), s.participantId())));
        assertThat(redisTemplate.hasKey("location:session:" + s.sessionId() + ":participant:" + s.participantId()))
                .isFalse();
        assertThatThrownBy(() -> locationService.latest(s.memberId(), s.sessionId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("캐시 유실은 DB에서 복구하고 30초 이상 된 유효 위치는 지연 상태로 표시한다")
    void latest_cacheMissAndOldPosition_recoversFromDatabaseAndMarksStale() {
        Scenario s = running();
        storedGps(s, 0);
        jdbcTemplate.update("update gps_events set received_at=now()-interval '31 seconds' where participant_id=?",
                s.participantId());
        locationCache.remove(s.sessionId(), s.participantId());
        assertThat(locationService.latest(s.memberId(), s.sessionId()).locations()).singleElement()
                .satisfies(location -> assertThat(location.isStale()).isTrue());
        Long outsider = member();
        assertThatThrownBy(() -> locationService.latest(outsider, s.sessionId())).isInstanceOf(BusinessException.class);
    }
}
