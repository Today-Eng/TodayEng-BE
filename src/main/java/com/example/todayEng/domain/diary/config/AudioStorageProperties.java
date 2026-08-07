package com.example.todayEng.domain.diary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.audio")
public record AudioStorageProperties(
        String directory,
        String publicUrlPrefix
) {
    public AudioStorageProperties {
        directory = directory == null || directory.isBlank()
                ? "./storage/audio"
                : directory;
        publicUrlPrefix = publicUrlPrefix == null || publicUrlPrefix.isBlank()
                ? "/files/audio"
                : publicUrlPrefix.replaceAll("/+$", "");
        if (publicUrlPrefix.isBlank()) {
            throw new IllegalArgumentException(
                    "storage.audio.public-url-prefix must not resolve to the root path");
        }
    }
}
