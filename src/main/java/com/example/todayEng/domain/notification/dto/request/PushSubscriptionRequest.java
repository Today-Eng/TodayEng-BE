package com.example.todayEng.domain.notification.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PushSubscriptionRequest(
        @NotBlank
        String endpoint,

        @Valid
        @NotNull
        PushSubscriptionKeys keys
) {

    public record PushSubscriptionKeys(
            @NotBlank
            String p256dh,

            @NotBlank
            String auth
    ) {
    }
}
