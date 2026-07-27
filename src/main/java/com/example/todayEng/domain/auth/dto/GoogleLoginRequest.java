package com.example.todayEng.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @Schema(description = "Google Identity Services에서 발급받은 ID Token", example = "google-id-token")
        @NotBlank(message = "Google ID 토큰은 필수입니다.")
        String idToken
) {
}
