package com.example.todayEng.domain.diary.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        URI baseUrl,
        String apiKey,
        String model,
        int connectTimeoutSeconds,
        int readTimeoutSeconds
) {
}
