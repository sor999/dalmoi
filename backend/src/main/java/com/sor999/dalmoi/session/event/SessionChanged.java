package com.sor999.dalmoi.session.event;

public record SessionChanged(Long sessionId, String eventType, Long memberId, Long participantId) {

    public SessionChanged(Long sessionId, String eventType, Long memberId) {
        this(sessionId, eventType, memberId, null);
    }
}
