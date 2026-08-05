package com.example.todayEng.domain.diary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "google.speech")
public record GoogleSpeechProperties(
        String languageCode,
        String audioEncoding,
        String contentType,
        String fileExtension,
        Resource credentialsLocation,
        long maxFileSizeBytes
) {
}
