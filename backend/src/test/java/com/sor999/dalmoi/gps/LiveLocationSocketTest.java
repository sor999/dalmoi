package com.sor999.dalmoi.gps;

import com.sor999.dalmoi.gps.dto.LocationResponse;
import com.sor999.dalmoi.gps.event.GpsStored;
import com.sor999.dalmoi.session.event.SessionChanged;
import com.sor999.dalmoi.support.MvpDatabaseTest;
import com.sor999.dalmoi.support.PostgresIntegrationTest;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@PostgresIntegrationTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LiveLocationSocketTest extends MvpDatabaseTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private JwtEncoder jwtEncoder;

    private WebSocketStompClient client;

    private final List<StompSession> sockets = new ArrayList<>();

    @BeforeEach
    void setUp() {
        client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new StringMessageConverter() {
            @Override
            protected boolean supportsMimeType(MessageHeaders headers) {
                return true;
            }
        });
    }

    @AfterEach
    void tearDown() {
        sockets.forEach(socket -> {
            if (socket.isConnected()) {
                socket.disconnect();
            }
        });
        client.stop();
    }

    @Test
    @DisplayName("커밋된 GPS만 실제 소켓에 전파하고 개인 종료 시 연결을 해제한다")
    void events_committedGpsAndFinish_deliversThenDisconnects() throws Exception {
        Scenario s = running();
        Handler handler = new Handler();
        StompSession socket = connect(tokenService.accessToken(s.memberId()), handler);
        BlockingQueue<String> messages = subscribe(socket, "/topic/sessions/" + s.sessionId() + "/locations");
        UUID id = UUID.randomUUID();
        Instant now = clock.instant();
        GpsStored event = new GpsStored(s.sessionId(), s.participantId(), new LocationResponse.Location(id,
                s.memberId(), 0, 37.52, 126.93, 5, now, now, now, false));
        transactionTemplate.executeWithoutResult(tx -> {
            eventPublisher.publishEvent(event);
            tx.setRollbackOnly();
        });
        assertThat(messages.poll(300, TimeUnit.MILLISECONDS)).isNull();
        transactionTemplate.executeWithoutResult(tx -> eventPublisher.publishEvent(event));
        assertThat(messages.poll(5, TimeUnit.SECONDS)).contains(id.toString());
        jdbcTemplate.update("update session_participants set status='FINISHED',ended_at=now() where id=?",
                s.participantId());
        transactionTemplate.executeWithoutResult(tx -> eventPublisher.publishEvent(
                new SessionChanged(s.sessionId(), "PARTICIPANT_FINISHED", s.memberId(), s.participantId())));
        handler.closed.get(5, TimeUnit.SECONDS);
        await().atMost(5, TimeUnit.SECONDS).until(() -> !socket.isConnected());
    }

    @Test
    @DisplayName("비참가자와 잘못된 토픽 구독 및 클라이언트 SEND를 거절한다")
    void subscribe_invalidMembershipOrDestination_rejectsFrames() throws Exception {
        Scenario s = running();
        Long outsider = member();
        for (String destination : List.of("/topic/sessions/" + s.sessionId() + "/locations", "/topic/arbitrary")) {
            Handler handler = new Handler();
            StompSession socket = connect(tokenService.accessToken(outsider), handler);
            socket.subscribe(destination, frames(new LinkedBlockingQueue<>()));
            handler.closed.get(5, TimeUnit.SECONDS);
        }
        Handler handler = new Handler();
        StompSession socket = connect(tokenService.accessToken(s.memberId()), handler);
        socket.send("/topic/sessions/" + s.sessionId() + "/locations", "fake GPS");
        handler.closed.get(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("이미 연결된 소켓도 Access Token이 만료되면 해제한다")
    void connection_expiredToken_closesSocket() throws Exception {
        Scenario s = running();
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("dalmoi")
                .subject(s.memberId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        Handler handler = new Handler();
        StompSession socket = connect(token, handler);
        handler.closed.get(6, TimeUnit.SECONDS);
        await().atMost(3, TimeUnit.SECONDS).until(() -> !socket.isConnected());
    }

    private StompSession connect(String token, Handler handler) throws Exception {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + token);
        StompSession socket = client
                .connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), headers, handler)
                .get(10, TimeUnit.SECONDS);
        sockets.add(socket);
        return socket;
    }

    private BlockingQueue<String> subscribe(StompSession socket, String destination) {
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        socket.subscribe(destination, frames(messages));
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            messagingTemplate.convertAndSend(destination, "ready");
            return messages.poll(100, TimeUnit.MILLISECONDS) != null;
        });
        messages.clear();
        return messages;
    }

    private StompFrameHandler frames(BlockingQueue<String> messages) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((String) payload);
            }
        };
    }

    private static class Handler extends StompSessionHandlerAdapter {

        private final CompletableFuture<Object> closed = new CompletableFuture<>();

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            closed.complete(payload);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            closed.complete(exception);
        }
    }
}
