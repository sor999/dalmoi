package com.sor999.dalmoi.gps.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dalmoi.gps")
public record GpsProperties(double maxAccuracyMeters, double maxSpeedMetersPerSecond, Duration maxPastAge,
        Duration maxFutureSkew, Duration staleAfter, Duration cacheTtl) {

    public GpsProperties {
        maxAccuracyMeters = maxAccuracyMeters <= 0 ? 50 : maxAccuracyMeters;
        maxSpeedMetersPerSecond = maxSpeedMetersPerSecond <= 0 ? 12 : maxSpeedMetersPerSecond;
        maxPastAge = maxPastAge == null ? Duration.ofMinutes(2) : maxPastAge;
        maxFutureSkew = maxFutureSkew == null ? Duration.ofSeconds(10) : maxFutureSkew;
        staleAfter = staleAfter == null ? Duration.ofSeconds(30) : staleAfter;
        cacheTtl = cacheTtl == null ? Duration.ofHours(1) : cacheTtl;
        if (!Double.isFinite(maxAccuracyMeters) || !Double.isFinite(maxSpeedMetersPerSecond)
                || maxPastAge.isNegative() || maxPastAge.isZero() || maxFutureSkew.isNegative()
                || staleAfter.isNegative() || staleAfter.isZero() || cacheTtl.toSeconds() < 1) {
            throw new IllegalArgumentException("GPS 설정 범위를 확인해 주세요.");
        }
    }
}
