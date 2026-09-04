package com.example.todayEng.domain.diary.storage;

import com.example.todayEng.domain.diary.config.GcsAudioStorageProperties;
import com.example.todayEng.domain.diary.config.GoogleSpeechProperties;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.google.auth.ServiceAccountSigner.SigningException;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "storage.audio.type", havingValue = "gcs")
public class GcsAudioFileStorage implements AudioFileStorage {

    private final Storage storage;
    private final GcsAudioStorageProperties properties;
    private final String answerFileExtension;
    private final String answerContentType;

    public GcsAudioFileStorage(Storage storage, GcsAudioStorageProperties properties,
            GoogleSpeechProperties speechProperties) {
        properties.validate();
        this.storage = storage;
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
                properties.sttPrefix(), diaryId, questionId, UUID.randomUUID(),
                answerFileExtension);
        put(key, audio, answerContentType, "no-store");
        return key;
    }

    @Override
    public byte[] read(String audioKey) {
        try {
            return storage.readAllBytes(properties.bucket(), audioKey);
        } catch (StorageException exception) {
            throw storageFailure(exception);
        }
    }

    @Override
    public String publicUrl(String audioKey) {
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(properties.bucket(), audioKey).build();
            URL signedUrl = storage.signUrl(
                    blobInfo,
                    properties.playbackUrlExpiration().toSeconds(),
                    TimeUnit.SECONDS,
                    Storage.SignUrlOption.withV4Signature());
            return signedUrl.toString();
        } catch (StorageException | SigningException | IllegalStateException exception) {
            throw storageFailure(exception);
        }
    }

    @Override
    public void deleteQuietly(String audioKey) {
        if (audioKey == null || audioKey.isBlank()) return;
        try {
            storage.delete(properties.bucket(), audioKey);
        } catch (StorageException exception) {
            log.warn("Failed to delete GCS audio: bucket={}, key={}, error={}",
                    properties.bucket(), audioKey, exception.getClass().getSimpleName());
        }
    }

    private void put(String key, byte[] audio, String contentType, String cacheControl) {
        BlobInfo blobInfo = BlobInfo.newBuilder(properties.bucket(), key)
                .setContentType(contentType)
                .setCacheControl(cacheControl)
                .build();
        try {
            storage.create(blobInfo, audio);
        } catch (StorageException exception) {
            throw storageFailure(exception);
        }
    }

    private BaseException storageFailure(RuntimeException exception) {
        log.error("GCS audio storage operation failed: bucket={}, error={}",
                properties.bucket(), exception.getClass().getSimpleName());
        return new BaseException(ErrorCode.AUDIO_STORAGE_FAILED);
    }
}
