package com.sor999.dalmoi.record.dto;

import com.sor999.dalmoi.course.dto.CoordinateRequest;
import com.sor999.dalmoi.course.service.GeoSupport;
import com.sor999.dalmoi.gps.domain.GpsEvent;
import com.sor999.dalmoi.record.domain.RunningRecord;
import com.sor999.dalmoi.session.domain.RunningSession;
import com.sor999.dalmoi.session.domain.SessionParticipant;
import java.time.Instant;
import java.util.List;

public record RecordResponse(Long recordId, Long participantId, Long sessionId, Long courseId, String courseTitle,
        Long crewId, String crewName, String sessionTitle, double distanceKm, double plannedDistanceKm,
        boolean plannedIsLoop, long durationSeconds, Instant startedAt, Instant endedAt,
        List<CoordinateRequest> plannedRoute, List<ActualPoint> actualRoute) {

    public record ActualPoint(double latitude, double longitude, Instant recordedAt) {
    }

    public record Summary(Long recordId, Long sessionId, Long courseId, String sessionTitle, Long crewId,
            String crewName, double distanceKm, long durationSeconds, Instant startedAt, Instant endedAt) {

        public static Summary from(RunningRecord record) {
            SessionParticipant p = record.getParticipant();
            RunningSession s = p.getSession();

            return new Summary(record.getId(), s.getId(), s.getCourse().getId(), s.getTitle(), s.getCrew().getId(),
                    s.getCrewNameSnapshot(), GeoSupport.km(record.getDistanceMeters()), record.getDurationSeconds(),
                    p.getStartedAt(), p.getEndedAt());
        }
    }

    public static RecordResponse of(RunningRecord record, List<GpsEvent> events) {
        SessionParticipant p = record.getParticipant();
        RunningSession s = p.getSession();

        return new RecordResponse(record.getId(), p.getId(), s.getId(), s.getCourse().getId(),
                s.getPlannedCourseTitle(), s.getCrew().getId(), s.getCrewNameSnapshot(), s.getTitle(),
                GeoSupport.km(record.getDistanceMeters()), GeoSupport.km(s.getPlannedDistanceMeters()),
                s.isPlannedLoop(), record.getDurationSeconds(), p.getStartedAt(), p.getEndedAt(),
                GeoSupport.points(s.getPlannedRoute()),
                events.stream()
                        .map(e -> new ActualPoint(e.getLocation().getY(), e.getLocation().getX(), e.getRecordedAt()))
                        .toList());
    }

    public record Status(Long participantId, String recordStatus, Long recordId, String errorCode) {
    }
}
