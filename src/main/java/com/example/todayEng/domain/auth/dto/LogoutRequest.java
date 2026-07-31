package com.example.todayEng.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @Schema(description = "로그인 응답에서 발급받은 Refresh Token", example = "발급받은-refresh-token")
        @NotBlank(message = "refreshToken은 필수입니다.")
        String refreshToken
) {
}
