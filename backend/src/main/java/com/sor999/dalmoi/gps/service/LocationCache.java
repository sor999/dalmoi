package com.sor999.dalmoi.gps.service;

import com.sor999.dalmoi.gps.config.GpsProperties;
import com.sor999.dalmoi.gps.dto.LocationResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocationCache {

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final GpsProperties properties;

    private final Set<String> dirty = new HashSet<>();

    private boolean initialized;

    private static final DefaultRedisScript<Long> UPDATE = new DefaultRedisScript<>("""
            local old = redis.call('HGET', KEYS[1], 'sequence')
            local next = ARGV[1]
            if old and (#old > #next or (#old == #next and old >= next)) then return 0 end
            redis.call('HSET', KEYS[1], 'sequence', next, 'payload', ARGV[2])
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            return 1
            """, Long.class);

    public synchronized void store(Long sessionId, Long participantId, LocationResponse.Location location) {
        String key = key(sessionId, participantId);
        try {
            initialize();
            write(key, location);
        } catch (RuntimeException exception) {
            dirty.add(key);
            log.warn("GPS 캐시 갱신 실패: 참가자 {}", participantId);
        }
    }

    public synchronized LocationResponse.Location latest(Long sessionId, Long participantId,
            Supplier<LocationResponse.Location> database) {
        String key = key(sessionId, participantId);
        try {
            initialize();
            if (!dirty.contains(key)) {
                Object cached = redisTemplate.opsForHash().get(key, "payload");
                if (cached != null) {
                    return objectMapper.readValue(cached.toString(), LocationResponse.Location.class);
                }
            }
        } catch (RuntimeException exception) {
            dirty.add(key);
            log.warn("GPS 캐시 조회 실패: 참가자 {}", participantId);
        }

        LocationResponse.Location location = database.get();
        try {
            initialize();
            if (location != null) {
                write(key, location);
            } else {
                redisTemplate.delete(key);
            }
            dirty.remove(key);
        } catch (RuntimeException exception) {
            dirty.add(key);
            log.warn("GPS 캐시 복구 실패: 참가자 {}", participantId);
        }
        return location;
    }

    public synchronized void remove(Long sessionId, Long participantId) {
        String key = key(sessionId, participantId);
        try {
            initialize();
            redisTemplate.delete(key);
            dirty.remove(key);
        } catch (RuntimeException exception) {
            dirty.add(key);
            log.warn("종료 위치 캐시 제거 실패: 참가자 {}", participantId);
        }
    }

    private void initialize() {
        if (initialized) {
            return;
        }
        // Single server: after restart no cached location is trusted until this scoped
        // cleanup succeeds.
        try (Cursor<String> keys = redisTemplate.scan(ScanOptions.scanOptions()
                .match("location:session:*:participant:*")
                .count(100)
                .build())) {
            while (keys.hasNext()) {
                redisTemplate.delete(keys.next());
            }
        }
        initialized = true;
    }

    private void write(String key, LocationResponse.Location location) {
        redisTemplate.execute(UPDATE, List.of(key), Long.toString(location.sequence()),
                objectMapper.writeValueAsString(location), Long.toString(properties.cacheTtl().toSeconds()));
    }

    private String key(Long sessionId, Long participantId) {
        return "location:session:" + sessionId + ":participant:" + participantId;
    }
}
