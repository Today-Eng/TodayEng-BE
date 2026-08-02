package com.example.todayEng.domain.user.dto.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SpotifyUserInfoResponse(

        @JsonProperty("account_id")
        String accountId,

        String id,

        String email,

        @JsonProperty("display_name")
        String displayName
) {
}
