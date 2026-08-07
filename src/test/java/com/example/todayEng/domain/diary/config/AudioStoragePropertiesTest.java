package com.example.todayEng.domain.diary.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AudioStoragePropertiesTest {

    @Test
    void normalizesTrailingSlashesFromPublicUrlPrefix() {
        AudioStorageProperties properties = new AudioStorageProperties("./audio", "/files/audio///");

        assertThat(properties.publicUrlPrefix()).isEqualTo("/files/audio");
    }

    @Test
    void rejectsPublicUrlPrefixThatResolvesToRoot() {
        assertThatThrownBy(() -> new AudioStorageProperties("./audio", "///"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not resolve to the root path");
    }
}
