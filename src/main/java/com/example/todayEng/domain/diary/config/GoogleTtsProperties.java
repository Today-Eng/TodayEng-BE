package com.example.todayEng.domain.diary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "google.tts")
public record GoogleTtsProperties(
        String languageCode,
        String voiceName,
        double speakingRate,
        String audioEncoding,
        Resource credentialsLocation
) {
}
