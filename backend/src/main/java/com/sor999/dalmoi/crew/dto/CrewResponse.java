package com.sor999.dalmoi.crew.dto;

import com.sor999.dalmoi.crew.domain.Crew;
import java.time.Instant;
import java.util.List;

public record CrewResponse(Long crewId, String name, Long ownerMemberId, long memberCount, int maxMembers,
        String myRole, List<MemberSummary> members, WaitingSession waitingSession) {

    public record MemberSummary(Long memberId, String nickname, String role) {
    }

    public record WaitingSession(Long sessionId, String title) {
    }

    public record Created(Long crewId, String name, Long ownerMemberId, String inviteCode, long memberCount,
            int maxMembers, Instant createdAt) {

        public static Created from(Crew crew) {
            return new Created(crew.getId(), crew.getName(), crew.getOwner().getId(), crew.getInviteCode(), 1,
                    crew.getMaxMembers(), crew.getCreatedAt());
        }
    }

    public record Joined(Long crewId, String name, Long crewMemberId, Long memberId, Instant joinedAt) {
    }

    public record InviteCode(Long crewId, String inviteCode) {
    }
}
