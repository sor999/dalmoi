package com.sor999.dalmoi.gps.service;

import com.sor999.dalmoi.gps.event.GpsStored;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocationBroadcastListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener
    public void stored(GpsStored event) {
        try {
            messagingTemplate.convertAndSend("/topic/sessions/" + event.sessionId() + "/locations", event.location());
        } catch (RuntimeException exception) {
            log.warn("GPS 실시간 전파 실패: {}", event.location().eventId(), exception);
        }
    }
}
