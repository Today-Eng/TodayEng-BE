package com.example.todayEng.domain.diary.storage;

import com.example.todayEng.domain.diary.config.AudioStorageProperties;
import com.example.todayEng.domain.diary.config.GoogleSpeechProperties;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "storage.audio.type", havingValue = "local", matchIfMissing = true)
public class LocalAudioFileStorage implements AudioFileStorage {

    private final Path rootDirectory;
    private final String publicUrlPrefix;
    private final String answerFileExtension;

    public LocalAudioFileStorage(AudioStorageProperties properties, GoogleSpeechProperties speechProperties) {
        this.rootDirectory = Path.of(properties.directory())
                .toAbsolutePath()
                .normalize();
        this.publicUrlPrefix = properties.publicUrlPrefix()
                .replaceAll("/+$", "");
        this.answerFileExtension = speechProperties.fileExtension();
    }

    @Override
    public String store(Long diaryId, Long questionId, byte[] audio) {
        String key = "diaries/%d/questions/%d/%s.mp3".formatted(
                diaryId,
                questionId,
                UUID.randomUUID()
        );
        Path target = resolveSafely(key);

        try {
            Files.createDirectories(target.getParent());
            Files.write(
                    target,
                    audio,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
            return key;
        } catch (IOException exception) {
            throw new BaseException(ErrorCode.AUDIO_STORAGE_FAILED);
        }
    }

    @Override
    public String storeAnswer(Long diaryId, Long questionId, byte[] audio) {
        String key = "diaries/%d/questions/%d/answers/%s.%s".formatted(
                diaryId, questionId, UUID.randomUUID(), answerFileExtension
        );
        Path target = resolveSafely(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, audio, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return key;
        } catch (IOException exception) {
            throw new BaseException(ErrorCode.AUDIO_STORAGE_FAILED);
        }
    }

    @Override
    public byte[] read(String audioKey) {
        try {
            return Files.readAllBytes(resolveSafely(audioKey));
        } catch (IOException exception) {
            throw new BaseException(ErrorCode.AUDIO_STORAGE_FAILED);
        }
    }

    @Override
    public String publicUrl(String audioKey) {
        return publicUrlPrefix + "/" + audioKey.replace('\\', '/');
    }

    @Override
    public void deleteQuietly(String audioKey) {
        try {
            Files.deleteIfExists(resolveSafely(audioKey));
        } catch (IOException | RuntimeException exception) {
            log.warn("Failed to delete TTS audio: key={}", audioKey);
        }
    }

    private Path resolveSafely(String key) {
        Path resolved = rootDirectory.resolve(key).normalize();
        if (!resolved.startsWith(rootDirectory)) {
            throw new BaseException(ErrorCode.AUDIO_STORAGE_FAILED);
        }
        return resolved;
    }
}
