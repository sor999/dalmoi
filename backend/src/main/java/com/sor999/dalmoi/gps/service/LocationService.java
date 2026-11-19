package com.sor999.dalmoi.gps.service;

import com.sor999.dalmoi.gps.config.GpsProperties;
import com.sor999.dalmoi.gps.domain.GpsEvent;
import com.sor999.dalmoi.gps.domain.GpsValidationStatus;
import com.sor999.dalmoi.gps.dto.LocationResponse;
import com.sor999.dalmoi.gps.repository.GpsEventRepository;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.global.exception.ErrorCode;
import com.sor999.dalmoi.session.domain.ParticipantStatus;
import com.sor999.dalmoi.session.domain.SessionStatus;
import com.sor999.dalmoi.session.domain.SessionParticipant;
import com.sor999.dalmoi.session.repository.SessionParticipantRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final SessionParticipantRepository sessionParticipantRepository;

    private final GpsEventRepository gpsEventRepository;

    private final LocationCache locationCache;

    private final GpsProperties properties;

    private final Clock clock;

    @Transactional(readOnly = true)
    public LocationResponse latest(Long memberId, Long sessionId) {
        SessionParticipant me = requireLive(memberId, sessionId);
        List<LocationResponse.Location> locations = new ArrayList<>();
        for (SessionParticipant p : sessionParticipantRepository.findBySessionIdOrderByJoinedAtAscIdAsc(sessionId)) {
            if (p.getStatus() != ParticipantStatus.RUNNING) {
                continue;
            }
            LocationResponse.Location location = locationCache.latest(sessionId, p.getId(),
                    () -> gpsEventRepository.findFirstByParticipantIdAndValidationStatusOrderBySequenceDesc(
                            p.getId(), GpsValidationStatus.VALID).map(this::location).orElse(null));
            if (location != null) {
                locations.add(location.stale(clock.instant(), properties.staleAfter().toSeconds()));
            }
        }
        return new LocationResponse(me.getSession().getId(), clock.instant(), locations);
    }

    public SessionParticipant requireLive(Long memberId, Long sessionId) {
        SessionParticipant p = sessionParticipantRepository.findBySessionIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_SESSION_PARTICIPANT));
        if (p.getStatus() != ParticipantStatus.RUNNING || p.getSession().getStatus() != SessionStatus.RUNNING) {
            throw new BusinessException(ErrorCode.PARTICIPANT_NOT_RUNNING);
        }
        return p;
    }

    public void remove(Long sessionId, Long participantId) {
        locationCache.remove(sessionId, participantId);
    }

    private LocationResponse.Location location(GpsEvent event) {
        return LocationResponse.Location.from(event, clock.instant(), properties.staleAfter().toSeconds());
    }
}
