package com.sor999.dalmoi.auth.dto;

public record RefreshResponse(String accessToken, long accessTokenExpiresInSeconds) {

    public static RefreshResponse of(String accessToken, long expires) {
        return new RefreshResponse(accessToken, expires);
    }
}
