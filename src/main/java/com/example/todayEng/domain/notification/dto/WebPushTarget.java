package com.example.todayEng.domain.notification.dto;

public record WebPushTarget(
        Long notificationSettingId,
        Long userId,
        String pushEndpoint,
        String p256dhKey,
        String authKey
) {
}
