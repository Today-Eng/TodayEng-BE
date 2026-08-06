package com.example.todayEng.domain.diary.storage;

import com.example.todayEng.domain.diary.config.GoogleSpeechProperties;
import com.example.todayEng.domain.diary.config.S3AudioStorageProperties;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Slf4j
@Component
@ConditionalOnProperty(name = "storage.audio.type", havingValue = "s3")
public class S3AudioFileStorage implements AudioFileStorage {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final S3AudioStorageProperties properties;
    private final String answerFileExtension;
    private final String answerContentType;

    public S3AudioFileStorage(S3Client s3Client, S3Presigner presigner,
            S3AudioStorageProperties properties, GoogleSpeechProperties speechProperties) {
        properties.validate();
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.properties = properties;
        this.answerFileExtension = speechProperties.fileExtension();
        this.answerContentType = speechProperties.contentType();
    }

    @Override
    public String store(Long diaryId, Long questionId, byte[] audio) {
        String key = "%s/diaries/%d/questions/%d/%s.mp3".formatted(
                properties.ttsPrefix(), diaryId, questionId, UUID.randomUUID());
        put(key, audio, "audio/mpeg", "private, max-age=31536000, immutable");
        return key;
    }

    @Override
    public String storeAnswer(Long diaryId, Long questionId, byte[] audio) {
        String key = "%s/diaries/%d/questions/%d/answers/%s.%s".formatted(
                properties.sttPrefix(), diaryId, questionId, UUID.randomUUID(), answerFileExtension);
        put(key, audio, answerContentType, "no-store");
        return key;
    }

    @Override
    public byte[] read(String audioKey) {
        try {
            ResponseBytes<GetObjectResponse> object = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(properties.bucket()).key(audioKey).build());
            return object.asByteArray();
        } catch (S3Exception | SdkClientException exception) {
            throw storageFailure(exception);
        }
    }

    @Override
    public String publicUrl(String audioKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.bucket()).key(audioKey).build();
            return presigner.presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(properties.playbackUrlExpiration())
                            .getObjectRequest(request)
                            .build())
                    .url().toString();
        } catch (S3Exception | SdkClientException exception) {
            throw storageFailure(exception);
        }
    }

    @Override
    public void deleteQuietly(String audioKey) {
        if (audioKey == null || audioKey.isBlank()) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket()).key(audioKey).build());
        } catch (S3Exception | SdkClientException exception) {
            log.warn("Failed to delete S3 audio: bucket={}, key={}, error={}",
                    properties.bucket(), audioKey, exception.getClass().getSimpleName());
        }
    }

    private void put(String key, byte[] audio, String contentType, String cacheControl) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(key)
                            .contentType(contentType)
                            .cacheControl(cacheControl)
                            .build(), RequestBody.fromBytes(audio));
        } catch (S3Exception | SdkClientException exception) {
            throw storageFailure(exception);
        }
    }

    private BaseException storageFailure(RuntimeException exception) {
        log.error("S3 audio storage operation failed: bucket={}, error={}",
                properties.bucket(), exception.getClass().getSimpleName());
        return new BaseException(ErrorCode.AUDIO_STORAGE_FAILED);
    }
}
