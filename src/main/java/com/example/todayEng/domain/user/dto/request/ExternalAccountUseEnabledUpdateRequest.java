package com.example.todayEng.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record ExternalAccountUseEnabledUpdateRequest(
        @NotNull(message = "사용 설정 값은 필수입니다.")
        Boolean useEnabled
) {
}
