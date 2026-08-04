package com.example.todayEng.domain.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "web-push.vapid")
public record WebPushProperties(
        String publicKey,
        String privateKey,
        String subject
) {
}
