package com.example.todayEng.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestLoginRequest(
        @Schema(description = "로컬 테스트용 사용자 식별자", example = "swagger-test-user")
        @NotBlank(message = "socialUid는 필수입니다.")
        @Size(max = 255, message = "socialUid는 255자 이하여야 합니다.")
        String socialUid
) {
}
