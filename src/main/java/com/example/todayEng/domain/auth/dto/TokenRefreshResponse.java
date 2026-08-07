package com.example.todayEng.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenRefreshResponse(
        @Schema(description = "새로 발급된 API 인증용 Access Token") String accessToken,
        @Schema(description = "Rotation으로 새로 발급된 Refresh Token") String refreshToken
) {
}
