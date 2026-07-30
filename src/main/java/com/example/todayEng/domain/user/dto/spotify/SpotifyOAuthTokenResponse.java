package com.example.todayEng.domain.user.dto.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SpotifyOAuthTokenResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("expires_in")
        Long expiresIn,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("scope")
        String scope
) {
}
