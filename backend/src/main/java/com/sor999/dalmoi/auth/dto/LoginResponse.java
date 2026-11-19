package com.sor999.dalmoi.auth.dto;

public record LoginResponse(String accessToken, String refreshToken, long accessTokenExpiresInSeconds) {

    public static LoginResponse of(String accessToken, String refreshToken, long expires) {
        return new LoginResponse(accessToken, refreshToken, expires);
    }
}
