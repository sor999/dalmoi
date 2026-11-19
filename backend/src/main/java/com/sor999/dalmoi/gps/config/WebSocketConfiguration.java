package com.sor999.dalmoi.gps.config;

import com.sor999.dalmoi.gps.service.SocketRegistry;
import com.sor999.dalmoi.gps.service.SocketAuthorizationService;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;

    private final SocketAuthorizationService authorizationService;

    private final SocketRegistry socketRegistry;

    private final Clock clock;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(16384)
                .setSendBufferSizeLimit(65536)
                .setSendTimeLimit(10000)
                .addDecoratorFactory(handler -> new WebSocketHandlerDecorator(handler) {

                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                        socketRegistry.open(session);
                        super.afterConnectionEstablished(session);
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                        socketRegistry.remove(session.getId());
                        super.afterConnectionClosed(session, status);
                    }
                });
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {

            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) {
                    throw new IllegalArgumentException("STOMP 프레임이 필요합니다.");
                }

                StompCommand command = accessor.getCommand();

                if (command == StompCommand.CONNECT) {
                    String header = accessor.getFirstNativeHeader("Authorization");

                    if (header == null || !header.startsWith("Bearer ")) {
                        throw new IllegalArgumentException("인증이 필요합니다.");
                    }

                    Jwt jwt = jwtDecoder.decode(header.substring(7));
                    accessor.setUser(new JwtAuthenticationToken(jwt));
                    socketRegistry.authenticate(accessor.getSessionId(), Long.valueOf(jwt.getSubject()),
                            jwt.getExpiresAt());
                } else if (command == StompCommand.SUBSCRIBE) {
                    if (!(accessor.getUser() instanceof JwtAuthenticationToken token)
                            || !token.getToken().getExpiresAt().isAfter(clock.instant())) {
                        throw new IllegalArgumentException("인증이 만료되었습니다.");
                    }

                    authorizationService.subscribe(Long.valueOf(token.getName()), accessor.getDestination());
                    socketRegistry.subscribe(accessor.getSessionId(), accessor.getDestination());
                    // Recheck after registration so a concurrent finish cannot miss closing this
                    // subscription.
                    authorizationService.subscribe(Long.valueOf(token.getName()), accessor.getDestination());
                } else if (command == StompCommand.SEND) {
                    throw new IllegalArgumentException("GPS는 REST로 전송해 주세요.");
                }

                return message;
            }
        });
    }
}
