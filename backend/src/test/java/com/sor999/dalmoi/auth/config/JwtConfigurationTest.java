package com.sor999.dalmoi.auth.config;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigurationTest {

    private final SecurityConfiguration configuration = new SecurityConfiguration(new ObjectMapper());

    private final String secret = Base64.getEncoder()
            .encodeToString("test-signing-key-for-dalmoi-32bytes".getBytes(StandardCharsets.UTF_8));

    private final Instant now = Instant.parse("2026-11-19T00:00:00Z");

    @ParameterizedTest(name = "유효하지 않은 키: {0}")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "not-base64!", "c2hvcnQ="})
    @DisplayName("누락·형식 오류·짧은 JWT 키는 기동 설정 단계에서 거절한다")
    void jwtKey_invalidSecret_rejectsConfiguration(String value) {
        assertThatThrownBy(() -> configuration.jwtKey(properties(value))).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("JWT는 서명·issuer·만료 경계·회원 식별자를 모두 검증한다")
    void jwtDecoder_claimsAndSignature_validatesEveryBoundary() {
        AuthProperties properties = properties(secret);
        JwtEncoder encoder = configuration.jwtEncoder(configuration.jwtKey(properties));
        JwtDecoder decoder = configuration.jwtDecoder(configuration.jwtKey(properties), properties,
                Clock.fixed(now, ZoneOffset.UTC));
        String valid = token(encoder, "dalmoi", "1", now.plusSeconds(1));
        assertThat(decoder.decode(valid).getSubject()).isEqualTo("1");
        for (String invalid : List.of(token(encoder, "wrong", "1", now.plusSeconds(1)),
                token(encoder, "dalmoi", "0", now.plusSeconds(1)), token(encoder, "dalmoi", "abc", now.plusSeconds(1)),
                token(encoder, "dalmoi", "1", now.minusSeconds(1)))) {
            assertThatThrownBy(() -> decoder.decode(invalid)).isInstanceOf(JwtException.class);
        }
        JwtDecoder atExpiry = configuration.jwtDecoder(configuration.jwtKey(properties), properties,
                Clock.fixed(now.plusSeconds(1), ZoneOffset.UTC));
        assertThatThrownBy(() -> atExpiry.decode(valid)).isInstanceOf(JwtException.class);
        JwtEncoder forged = configuration.jwtEncoder(configuration.jwtKey(properties(Base64.getEncoder()
                .encodeToString(
                        "different-signing-key-32-bytes-long!".getBytes(StandardCharsets.UTF_8)))));
        assertThatThrownBy(() -> decoder.decode(token(forged, "dalmoi", "1", now.plusSeconds(1))))
                .isInstanceOf(JwtException.class);
    }

    private AuthProperties properties(String value) {
        return new AuthProperties(value, "dalmoi", Duration.ofMinutes(30), Duration.ofDays(14));
    }

    private String token(JwtEncoder encoder, String issuer, String subject, Instant expiry) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(now.minusSeconds(5))
                .expiresAt(expiry)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
