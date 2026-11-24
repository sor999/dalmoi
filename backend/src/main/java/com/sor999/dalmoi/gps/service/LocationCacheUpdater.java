package com.sor999.dalmoi.gps.service;

import com.sor999.dalmoi.gps.event.GpsStored;
import com.sor999.dalmoi.session.event.SessionChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LocationCacheUpdater {

    private final LocationCache locationCache;

    @TransactionalEventListener
    public void stored(GpsStored event) {
        locationCache.store(event.sessionId(), event.participantId(), event.location());
    }

    @TransactionalEventListener
    public void changed(SessionChanged event) {
        if (event.eventType().equals("PARTICIPANT_FINISHED") && event.participantId() != null) {
            locationCache.remove(event.sessionId(), event.participantId());
        }
    }
}
