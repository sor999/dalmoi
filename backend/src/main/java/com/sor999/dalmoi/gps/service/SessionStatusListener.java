package com.sor999.dalmoi.gps.service;

import com.sor999.dalmoi.session.event.SessionChanged;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionStatusListener {

    private final SimpMessagingTemplate messagingTemplate;

    private final SocketRegistry socketRegistry;

    private final Clock clock;

    public record StatusEvent(Long sessionId, String eventType, Long memberId, Instant receivedAt) {
    }

    @TransactionalEventListener
    public void changed(SessionChanged event) {
        try {
            messagingTemplate.convertAndSend("/topic/sessions/" + event.sessionId() + "/status",
                    new StatusEvent(event.sessionId(), event.eventType(), event.memberId(), clock.instant()));
        } catch (RuntimeException exception) {
            log.warn("러닝 상태 전파 실패: {}", event.sessionId(), exception);
        }

        if (event.eventType().equals("PARTICIPANT_FINISHED")) {
            socketRegistry.stop(event.sessionId(), event.memberId());
        }

        if (event.eventType().equals("SESSION_COMPLETED")) {
            socketRegistry.stop(event.sessionId(), null);
        }
    }
}
