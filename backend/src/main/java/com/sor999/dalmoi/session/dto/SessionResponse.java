package com.sor999.dalmoi.session.dto;

import com.sor999.dalmoi.course.dto.CoordinateRequest;
import java.time.Instant;
import java.util.List;

public record SessionResponse(Long sessionId, Long crewId, String crewName, String title, String status,
        CourseSnapshot course, long crewMemberCount, long participantCount, List<MemberStatus> members,
        List<Participant> participants, String myParticipationStatus, Instant myStartedAt, boolean canStartSession,
        boolean canJoinSession, Instant createdAt, Instant startedAt, Instant completedAt) {

    public record CourseSnapshot(Long courseId, String title, double estimatedDistanceKm, boolean isLoop,
            List<CoordinateRequest> points) {
    }

    public record MemberStatus(Long memberId, String nickname, String role, String participationStatus) {
    }

    public record Participant(Long participantId, Long memberId, String nickname, String status, Instant joinedAt,
            Instant startedAt, Instant endedAt) {
    }

    public record Summary(Long sessionId, String title, String status, Long courseId, long participantCount) {
    }

    public record Command(Long sessionId, String status, Instant startedAt, Instant completedAt) {
    }

    public record Personal(Long participantId, Long sessionId, String status, Instant startedAt, Instant endedAt,
            String recordStatus) {
    }
}
