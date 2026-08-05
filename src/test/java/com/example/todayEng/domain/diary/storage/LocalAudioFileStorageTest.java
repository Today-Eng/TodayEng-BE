package com.example.todayEng.domain.diary.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.config.AudioStorageProperties;
import com.example.todayEng.domain.diary.config.GoogleSpeechProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalAudioFileStorageTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesMp3UnderConfiguredDirectory() throws Exception {
        LocalAudioFileStorage storage = new LocalAudioFileStorage(
                new AudioStorageProperties(
                        tempDirectory.toString(),
                        "/files/audio"
                ),
                new GoogleSpeechProperties("en-US", "WEBM_OPUS", "audio/webm", "webm", null, 10)
        );

        String key = storage.store(10L, 101L, new byte[]{1, 2, 3});

        assertThat(key).startsWith("diaries/10/questions/101/")
                .endsWith(".mp3");
        assertThat(Files.readAllBytes(tempDirectory.resolve(key)))
                .containsExactly(1, 2, 3);
        assertThat(storage.publicUrl(key))
                .isEqualTo("/files/audio/" + key);
    }
}
