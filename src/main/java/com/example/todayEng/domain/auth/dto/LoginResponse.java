package com.example.todayEng.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "API 인증용 Access Token") String accessToken,
        @Schema(description = "로그아웃 및 토큰 재발급용 Refresh Token") String refreshToken,
        @Schema(description = "최초 가입 여부", example = "true") boolean isNewUser
) {
}
