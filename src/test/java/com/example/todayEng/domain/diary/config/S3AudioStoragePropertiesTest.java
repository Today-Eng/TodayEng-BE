package com.example.todayEng.domain.diary.config;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class S3AudioStoragePropertiesTest {

    @Test
    void rejectsPrefixesThatNormalizeToEmpty() {
        S3AudioStorageProperties tts = properties("/", "stt", Duration.ofMinutes(30));
        S3AudioStorageProperties stt = properties("tts", "///", Duration.ofMinutes(30));

        assertThatThrownBy(tts::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tts-prefix");
        assertThatThrownBy(stt::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stt-prefix");
    }

    @Test
    void acceptsSevenDayPlaybackExpiration() {
        assertThatNoException().isThrownBy(
                () -> properties("tts", "stt", Duration.ofDays(7)).validate());
    }

    @Test
    void rejectsPlaybackExpirationLongerThanSevenDays() {
        S3AudioStorageProperties properties = properties("tts", "stt", Duration.ofDays(8));

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not exceed 7 days");
    }

    private S3AudioStorageProperties properties(
            String ttsPrefix, String sttPrefix, Duration playbackUrlExpiration) {
        return new S3AudioStorageProperties(
                "todayeng-test", "ap-northeast-2", ttsPrefix, sttPrefix, playbackUrlExpiration);
    }
}
