package com.example.todayEng.domain.notification.dto;

public record WebPushPayload(
        String title,
        String body,
        String url
) {
}
