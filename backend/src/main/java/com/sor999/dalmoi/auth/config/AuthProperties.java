package com.sor999.dalmoi.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dalmoi.auth")
public record AuthProperties(String secret, String issuer, Duration accessTtl, Duration refreshTtl) {

    public AuthProperties {
        issuer = issuer == null ? "dalmoi" : issuer;
        accessTtl = accessTtl == null ? Duration.ofMinutes(30) : accessTtl;
        refreshTtl = refreshTtl == null ? Duration.ofDays(14) : refreshTtl;

        if (accessTtl.isNegative() || accessTtl.isZero() || refreshTtl.isNegative() || refreshTtl.isZero()) {
            throw new IllegalArgumentException("토큰 유효기간은 양수여야 합니다.");
        }
    }
}
