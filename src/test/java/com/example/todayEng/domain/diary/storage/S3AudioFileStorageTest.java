package com.example.todayEng.domain.diary.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.todayEng.domain.diary.config.GoogleSpeechProperties;
import com.example.todayEng.domain.diary.config.S3AudioStorageProperties;
import java.net.URL;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3AudioFileStorageTest {

    @Mock S3Client s3Client;
    @Mock S3Presigner presigner;

    private S3AudioFileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new S3AudioFileStorage(
                s3Client,
                presigner,
                new S3AudioStorageProperties(
                        "todayeng-test-media", "ap-northeast-2", "/tts/", "/stt/", Duration.ofMinutes(15)),
                new GoogleSpeechProperties(
                        "en-US", "WEBM_OPUS", "audio/webm", "webm", null, 10_485_760));
    }

    @Test
    void storesTtsAsPrivateMp3UnderTtsPrefix() {
        String key = storage.store(10L, 101L, new byte[]{1, 2, 3});

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(key).startsWith("tts/diaries/10/questions/101/").endsWith(".mp3");
        assertThat(request.getValue().bucket()).isEqualTo("todayeng-test-media");
        assertThat(request.getValue().key()).isEqualTo(key);
        assertThat(request.getValue().contentType()).isEqualTo("audio/mpeg");
        assertThat(request.getValue().cacheControl()).contains("private");
    }

    @Test
    void storesSttInputAsNonCacheableWebmUnderSttPrefix() {
        String key = storage.storeAnswer(10L, 101L, new byte[]{1});

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(key).startsWith("stt/diaries/10/questions/101/answers/").endsWith(".webm");
        assertThat(request.getValue().contentType()).isEqualTo("audio/webm");
        assertThat(request.getValue().cacheControl()).isEqualTo("no-store");
    }

    @Test
    void readsAndDeletesObjectByStoredKey() {
        String key = "stt/diaries/10/questions/101/answers/audio.webm";
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[]{4, 5}));

        assertThat(storage.read(key)).containsExactly(4, 5);
        storage.deleteQuietly(key);

        ArgumentCaptor<GetObjectRequest> get = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(get.capture());
        assertThat(get.getValue().key()).isEqualTo(key);
        ArgumentCaptor<DeleteObjectRequest> delete = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(delete.capture());
        assertThat(delete.getValue().key()).isEqualTo(key);
    }

    @Test
    void createsExpiringPlaybackUrlWithoutPersistingIt() throws Exception {
        PresignedGetObjectRequest signed = PresignedGetObjectRequest.builder()
                .isBrowserExecutable(true)
                .httpRequest(software.amazon.awssdk.http.SdkHttpFullRequest.builder()
                        .method(software.amazon.awssdk.http.SdkHttpMethod.GET)
                        .uri(new URL("https://example.com/signed-audio").toURI())
                        .build())
                .signedHeaders(java.util.Map.of("host", java.util.List.of("example.com")))
                .expiration(java.time.Instant.now().plus(Duration.ofMinutes(15)))
                .build();
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(signed);

        assertThat(storage.publicUrl("tts/audio.mp3"))
                .isEqualTo("https://example.com/signed-audio");

        ArgumentCaptor<GetObjectPresignRequest> request =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(presigner).presignGetObject(request.capture());
        assertThat(request.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(request.getValue().getObjectRequest().key()).isEqualTo("tts/audio.mp3");
    }
}
