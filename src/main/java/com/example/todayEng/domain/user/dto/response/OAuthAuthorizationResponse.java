package com.example.todayEng.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record OAuthAuthorizationResponse(
        @Schema(description = "Google 인가 URL", example = "https://accounts.google.com/o/oauth2/v2/auth?...")
        String authorizationUrl
) {
}