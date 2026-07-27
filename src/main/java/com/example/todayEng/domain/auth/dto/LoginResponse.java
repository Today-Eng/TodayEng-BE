package com.example.todayEng.domain.auth.dto;

public record LoginResponse(String accessToken, String refreshToken, boolean isNewUser) {
}
