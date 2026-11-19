package com.sor999.dalmoi.gps.service;

import com.sor999.dalmoi.course.service.GeoSupport;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.global.exception.ErrorCode;
import com.sor999.dalmoi.gps.config.GpsProperties;
import com.sor999.dalmoi.gps.event.GpsStored;
import com.sor999.dalmoi.gps.domain.GpsEvent;
import com.sor999.dalmoi.gps.domain.GpsValidationStatus;
import com.sor999.dalmoi.gps.dto.GpsRequest;
import com.sor999.dalmoi.gps.dto.LocationResponse;
import com.sor999.dalmoi.gps.repository.GpsEventRepository;
import com.sor999.dalmoi.session.domain.ParticipantStatus;
import com.sor999.dalmoi.session.domain.SessionParticipant;
import com.sor999.dalmoi.session.domain.SessionStatus;
import com.sor999.dalmoi.session.repository.SessionParticipantRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GpsService {

    private final GpsEventRepository gpsEventRepository;

    private final SessionParticipantRepository sessionParticipantRepository;

    private final GpsProperties properties;

    private final Clock clock;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public GpsEvent receive(Long memberId, Long sessionId, GpsRequest request) {
        SessionParticipant participant = sessionParticipantRepository.findLockedBySessionAndMember(sessionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_SESSION_PARTICIPANT));
        GpsEvent existing = gpsEventRepository.findByEventId(request.eventId()).orElse(null);

        if (existing != null) {
            if (!existing.getParticipant().getId().equals(participant.getId()) || !existing.matches(request.sequence(),
                    request.latitude(), request.longitude(), request.accuracyMeters(), request.recordedAt())) {
                throw new BusinessException(ErrorCode.GPS_EVENT_CONFLICT);
            }

            return existing;
        }

        if (participant.getStatus() != ParticipantStatus.RUNNING
                || participant.getSession().getStatus() != SessionStatus.RUNNING) {
            throw new BusinessException(ErrorCode.PARTICIPANT_NOT_RUNNING);
        }

        Instant now = clock.instant();

        if (!Double.isFinite(request.accuracyMeters()) || !Double.isFinite(request.latitude())
                || !Double.isFinite(request.longitude())
                || request.recordedAt().isBefore(now.minus(properties.maxPastAge()))
                || request.recordedAt().isAfter(now.plus(properties.maxFutureSkew()))
                || request.recordedAt().isBefore(participant.getStartedAt())) {
            throw new BusinessException(ErrorCode.INVALID_GPS_PAYLOAD);
        }

        if (gpsEventRepository.findByParticipantIdAndSequence(participant.getId(), request.sequence()).isPresent()) {
            throw new BusinessException(ErrorCode.GPS_SEQUENCE_CONFLICT);
        }

        GpsEvent last = gpsEventRepository.findFirstByParticipantIdOrderBySequenceDesc(participant.getId())
                .orElse(null);
        GpsEvent event = GpsEvent.create(request.eventId(), participant, request.sequence(),
                gpsEventRepository.maxIngestOrder(participant.getId()) + 1,
                GeoSupport.FACTORY.createPoint(new Coordinate(request.longitude(), request.latitude())),
                request.accuracyMeters(), request.recordedAt().truncatedTo(ChronoUnit.MICROS), now);
        String reason = rejectionReason(event, last, participant.getId());

        if (reason == null) {
            event.validate(now);
        } else {
            event.reject(reason, now);
        }

        gpsEventRepository.saveAndFlush(event);

        if (event.getValidationStatus() == GpsValidationStatus.VALID) {
            eventPublisher.publishEvent(new GpsStored(sessionId, participant.getId(),
                    LocationResponse.Location.from(event, now,
                            properties.staleAfter().toSeconds())));
        }

        return event;
    }

    private String rejectionReason(GpsEvent event, GpsEvent last, Long participantId) {
        if (last != null && event.getSequence() <= last.getSequence()) {
            return "LATE_SEQUENCE";
        }

        if (event.getAccuracyMeters() > properties.maxAccuracyMeters()) {
            return "LOW_ACCURACY";
        }

        GpsEvent previous = gpsEventRepository
                .findFirstByParticipantIdAndValidationStatusOrderBySequenceDesc(participantId,
                        GpsValidationStatus.VALID)
                .orElse(null);

        if (previous == null) {
            return null;
        }

        double seconds = Duration.between(previous.getRecordedAt(), event.getRecordedAt()).toMillis() / 1000.0;
        double meters = GeoSupport.distance(previous.getLocation().getCoordinate(),
                event.getLocation().getCoordinate());

        if (seconds < 0 || (seconds == 0 && meters > 0)) {
            return "TIME_REVERSED";
        }

        if (seconds > 0 && meters / seconds > properties.maxSpeedMetersPerSecond()) {
            return "UNREALISTIC_SPEED";
        }

        return null;
    }
}
