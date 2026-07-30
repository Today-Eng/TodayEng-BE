package com.example.todayEng.domain.user.dto.google;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserInfoResponse(

        String sub,

        String email,

        @JsonProperty("email_verified")
        Boolean emailVerified
) {
}