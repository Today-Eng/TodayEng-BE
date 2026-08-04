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
                : publicUrlPrefix;
    }
}
