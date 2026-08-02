package com.example.todayEng.domain.notification.dto.request;

import jakarta.validation.constraints.NotNull;

public record NotificationEnabledRequest(
        @NotNull
        Boolean isEnabled
) {
}
