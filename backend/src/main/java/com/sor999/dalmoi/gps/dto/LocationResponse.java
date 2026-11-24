package com.sor999.dalmoi.gps.dto;

import com.sor999.dalmoi.gps.domain.GpsEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LocationResponse(Long sessionId, Instant serverTime, List<Location> locations) {

    public record Location(UUID eventId, Long memberId, long sequence, double latitude, double longitude,
            double accuracyMeters, Instant recordedAt, Instant receivedAt, Instant lastReceivedAt, boolean isStale) {

        public static Location from(GpsEvent event, Instant now, long staleSeconds) {
            return new Location(event.getEventId(), event.getParticipant().getMember().getId(), event.getSequence(),
                    event.getLocation().getY(), event.getLocation().getX(), event.getAccuracyMeters(),
                    event.getRecordedAt(), event.getReceivedAt(), event.getReceivedAt(),
                    !event.getReceivedAt().plusSeconds(staleSeconds).isAfter(now));
        }

        public Location stale(Instant now, long seconds) {
            return new Location(eventId, memberId, sequence, latitude, longitude, accuracyMeters, recordedAt,
                    receivedAt, lastReceivedAt, !lastReceivedAt.plusSeconds(seconds).isAfter(now));
        }
    }

    public record Accepted(UUID eventId, boolean accepted, String processingStatus) {
    }
}
