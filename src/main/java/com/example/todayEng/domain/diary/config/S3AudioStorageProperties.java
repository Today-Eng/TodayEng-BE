package com.example.todayEng.domain.diary.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.audio.s3")
public record S3AudioStorageProperties(
        String bucket,
        String region,
        String ttsPrefix,
        String sttPrefix,
        Duration playbackUrlExpiration
) {
    private static final Duration MAX_PLAYBACK_URL_EXPIRATION = Duration.ofDays(7);

    public S3AudioStorageProperties {
        region = defaultIfBlank(region, "ap-northeast-2");
        ttsPrefix = normalizePrefix(defaultIfBlank(ttsPrefix, "tts"));
        sttPrefix = normalizePrefix(defaultIfBlank(sttPrefix, "stt"));
        playbackUrlExpiration = playbackUrlExpiration == null
                ? Duration.ofMinutes(30)
                : playbackUrlExpiration;
    }

    public void validate() {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("storage.audio.s3.bucket must be configured for S3 audio storage");
        }
        if (ttsPrefix.isBlank()) {
            throw new IllegalStateException("storage.audio.s3.tts-prefix must not resolve to an empty prefix");
        }
        if (sttPrefix.isBlank()) {
            throw new IllegalStateException("storage.audio.s3.stt-prefix must not resolve to an empty prefix");
        }
        if (playbackUrlExpiration.isZero() || playbackUrlExpiration.isNegative()) {
            throw new IllegalStateException("storage.audio.s3.playback-url-expiration must be positive");
        }
        if (playbackUrlExpiration.compareTo(MAX_PLAYBACK_URL_EXPIRATION) > 0) {
            throw new IllegalStateException(
                    "storage.audio.s3.playback-url-expiration must not exceed 7 days");
        }
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String normalizePrefix(String prefix) {
        return prefix.replace('\\', '/').replaceAll("^/+|/+$", "");
    }
}
