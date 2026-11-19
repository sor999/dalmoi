package com.sor999.dalmoi.gps.event;

import com.sor999.dalmoi.gps.dto.LocationResponse;

public record GpsStored(Long sessionId, Long participantId, LocationResponse.Location location) {
}
