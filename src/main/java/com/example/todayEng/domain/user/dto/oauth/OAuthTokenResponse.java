package com.example.todayEng.domain.user.dto.oauth;

public record OAuthTokenResponse(
        String accessToken,
        String refreshToken,
        Long expiresInSeconds
) {
}