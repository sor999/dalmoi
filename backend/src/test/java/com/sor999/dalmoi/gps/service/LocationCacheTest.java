package com.sor999.dalmoi.gps.service;

import com.sor999.dalmoi.gps.config.GpsProperties;
import com.sor999.dalmoi.gps.dto.LocationResponse;
import com.sor999.dalmoi.support.MvpIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MvpIntegrationTest
class LocationCacheTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GpsProperties properties;

    @Test
    @DisplayName("Lua 갱신은 큰 정수 sequence와 TTL을 보존하고 중복·역순을 무시한다")
    void store_largeSequences_preservesNewestValueAndTtl() {
        LocationCache cache = new LocationCache(redisTemplate, objectMapper, properties);
        long newest = 9007199254740993L;
        cache.store(100L, 200L, location(newest));
        String key = "location:session:100:participant:200";
        redisTemplate.expire(key, Duration.ofSeconds(30));
        cache.store(100L, 200L, location(newest - 1));
        cache.store(100L, 200L, location(newest));
        assertThat(cache.latest(100L, 200L, () -> {
            throw new AssertionError("캐시 적중이어야 합니다.");
        }).sequence())
                .isEqualTo(newest);
        assertThat(redisTemplate.getExpire(key)).isBetween(1L, 30L);
        redisTemplate.delete(key);
        assertThat(cache.latest(100L, 200L, () -> location(newest)).sequence()).isEqualTo(newest);
        assertThat(redisTemplate.getExpire(key)).isGreaterThan(3500);
    }

    @Test
    @DisplayName("갱신 실패 뒤 남은 과거 캐시를 우회하고 DB 최신값으로 복구한다")
    @SuppressWarnings("unchecked")
    void latest_failedWrite_bypassesOldCacheAndRepairsIt() {
        StringRedisTemplate faulty = spy(redisTemplate);
        LocationCache cache = new LocationCache(faulty, objectMapper, properties);
        cache.store(101L, 201L, location(1));
        doThrow(new IllegalStateException("Redis unavailable")).when(faulty)
                .execute(any(DefaultRedisScript.class), anyList(), any(), any(), any());
        cache.store(101L, 201L, location(2));
        assertThat(redisTemplate.opsForHash().get("location:session:101:participant:201", "sequence")).isEqualTo("1");
        doCallRealMethod().when(faulty).execute(any(DefaultRedisScript.class), anyList(), any(), any(), any());
        AtomicBoolean databaseUsed = new AtomicBoolean();
        assertThat(cache.latest(101L, 201L, () -> {
            databaseUsed.set(true);
            return location(2);
        }).sequence()).isEqualTo(2);
        assertThat(databaseUsed).isTrue();
        assertThat(cache.latest(101L, 201L, () -> {
            throw new AssertionError("복구 후 캐시를 사용해야 합니다.");
        }).sequence()).isEqualTo(2);
    }

    @Test
    @DisplayName("재시작 시 위치 키만 정리하고 다른 Redis 데이터는 보존한다")
    void latest_newInstance_discardsOldLocationsOnly() {
        redisTemplate.opsForValue().set("mvp:test:unrelated", "keep");
        redisTemplate.opsForHash().put("location:session:102:participant:202", "payload", "old");
        LocationCache restarted = new LocationCache(redisTemplate, objectMapper, properties);
        assertThat(restarted.latest(102L, 202L, () -> location(3)).sequence()).isEqualTo(3);
        assertThat(redisTemplate.opsForValue().get("mvp:test:unrelated")).isEqualTo("keep");
        redisTemplate.delete("mvp:test:unrelated");
    }

    private LocationResponse.Location location(long sequence) {
        Instant now = Instant.now();
        return new LocationResponse.Location(UUID.randomUUID(), 1L, sequence, 37.52, 126.93, 5,
                now, now, now, false);
    }
}
