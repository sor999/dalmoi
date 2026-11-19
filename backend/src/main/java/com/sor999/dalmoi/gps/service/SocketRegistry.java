package com.sor999.dalmoi.gps.service;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

@Component
@Slf4j
@RequiredArgsConstructor
public class SocketRegistry {

    private final Clock clock;

    private final ConcurrentMap<String, Connection> connections = new ConcurrentHashMap<>();

    private static final class Connection {

        final WebSocketSession socket;

        final Instant openedAt;

        volatile Long memberId;

        volatile Instant expiresAt;

        final Set<String> destinations = ConcurrentHashMap.newKeySet();

        Connection(WebSocketSession socket, Instant now) {
            this.socket = socket;
            this.openedAt = now;
        }
    }

    public void open(WebSocketSession socket) {
        connections.put(socket.getId(), new Connection(socket, clock.instant()));
    }

    public void remove(String id) {
        connections.remove(id);
    }

    public void authenticate(String id, Long memberId, Instant expiresAt) {
        Connection connection = connections.get(id);

        if (connection == null) {
            throw new IllegalArgumentException("연결이 종료되었습니다.");
        }

        connection.memberId = memberId;
        connection.expiresAt = expiresAt;
    }

    public void subscribe(String id, String destination) {
        Connection c = connections.get(id);

        if (c != null) {
            c.destinations.add(destination);
        }
    }

    public void stop(Long sessionId, Long memberId) {
        connections.forEach((id, c) -> {
            if ((memberId == null || memberId.equals(c.memberId))
                    && c.destinations.stream().anyMatch(d -> d.startsWith("/topic/sessions/" + sessionId + "/"))) {
                close(id, c);
            }
        });
    }

    @Scheduled(fixedDelay = 1000)
    public void expire() {
        Instant now = clock.instant();
        connections.forEach((id, c) -> {
            if ((c.expiresAt != null && !c.expiresAt.isAfter(now))
                    || (c.memberId == null && c.openedAt.plusSeconds(15).isBefore(now))) {
                close(id, c);
            }
        });
    }

    private void close(String id, Connection c) {
        connections.remove(id, c);

        try {
            c.socket.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException exception) {
            log.warn("WebSocket 연결 종료 실패: {}", id, exception);
        }
    }
}
