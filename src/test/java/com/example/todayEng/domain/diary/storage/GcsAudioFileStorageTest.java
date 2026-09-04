package com.example.todayEng.domain.diary.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.todayEng.domain.diary.config.GcsAudioStorageProperties;
import com.example.todayEng.domain.diary.config.GoogleSpeechProperties;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GcsAudioFileStorageTest {

    @Mock Storage gcsStorage;

    private GcsAudioFileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new GcsAudioFileStorage(
                gcsStorage,
                new GcsAudioStorageProperties(
                        "todayeng-test-media", "/tts/", "/stt/", Duration.ofMinutes(15)),
                new GoogleSpeechProperties(
                        "en-US", "WEBM_OPUS", "audio/webm", "webm", null, 10_485_760));
    }

    @Test
    void storesTtsAsPrivateMp3UnderTtsPrefix() {
        String key = storage.store(10L, 101L, new byte[]{1, 2, 3});

        ArgumentCaptor<BlobInfo> blob = ArgumentCaptor.forClass(BlobInfo.class);
        verify(gcsStorage).create(blob.capture(), eq(new byte[]{1, 2, 3}));
        assertThat(key).startsWith("tts/diaries/10/questions/101/").endsWith(".mp3");
        assertThat(blob.getValue().getBucket()).isEqualTo("todayeng-test-media");
        assertThat(blob.getValue().getName()).isEqualTo(key);
        assertThat(blob.getValue().getContentType()).isEqualTo("audio/mpeg");
        assertThat(blob.getValue().getCacheControl()).contains("private");
    }

    @Test
    void storesSttInputAsNonCacheableWebmUnderSttPrefix() {
        String key = storage.storeAnswer(10L, 101L, new byte[]{1});

        ArgumentCaptor<BlobInfo> blob = ArgumentCaptor.forClass(BlobInfo.class);
        verify(gcsStorage).create(blob.capture(), eq(new byte[]{1}));
        assertThat(key).startsWith("stt/diaries/10/questions/101/answers/").endsWith(".webm");
        assertThat(blob.getValue().getContentType()).isEqualTo("audio/webm");
        assertThat(blob.getValue().getCacheControl()).isEqualTo("no-store");
    }

    @Test
    void readsAndDeletesObjectByStoredKey() {
        String key = "stt/diaries/10/questions/101/answers/audio.webm";
        when(gcsStorage.readAllBytes("todayeng-test-media", key)).thenReturn(new byte[]{4, 5});

        assertThat(storage.read(key)).containsExactly(4, 5);
        storage.deleteQuietly(key);

        verify(gcsStorage).readAllBytes("todayeng-test-media", key);
        verify(gcsStorage).delete("todayeng-test-media", key);
    }

    @Test
    void createsV4ExpiringPlaybackUrlWithoutPersistingIt() throws Exception {
        when(gcsStorage.signUrl(
                any(BlobInfo.class),
                eq(900L),
                eq(TimeUnit.SECONDS),
                any(Storage.SignUrlOption.class)))
                .thenReturn(new URL("https://storage.googleapis.com/signed-audio"));

        assertThat(storage.publicUrl("tts/audio.mp3"))
                .isEqualTo("https://storage.googleapis.com/signed-audio");

        ArgumentCaptor<BlobInfo> blob = ArgumentCaptor.forClass(BlobInfo.class);
        verify(gcsStorage).signUrl(
                blob.capture(),
                eq(900L),
                eq(TimeUnit.SECONDS),
                any(Storage.SignUrlOption.class));
        assertThat(blob.getValue().getBucket()).isEqualTo("todayeng-test-media");
        assertThat(blob.getValue().getName()).isEqualTo("tts/audio.mp3");
    }
}
