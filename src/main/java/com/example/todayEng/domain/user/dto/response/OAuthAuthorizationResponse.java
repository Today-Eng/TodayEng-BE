package com.example.todayEng.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record OAuthAuthorizationResponse(
        @Schema(description = "외부 서비스 인가 URL", example = "https://accounts.spotify.com/authorize?...")
        String authorizationUrl
) {
}
