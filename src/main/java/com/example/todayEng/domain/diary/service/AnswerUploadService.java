package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.response.AnswerUploadResponse;
import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.storage.AudioFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerUploadService {
    private final AudioUploadValidator validator;
    private final AnswerPersistenceService persistenceService;
    private final AudioFileStorage storage;
    private final SpeechTranscriptionAsyncService asyncService;

    public AnswerUploadResponse upload(Long userId, Long diaryId, Long questionId, MultipartFile file) {
        AudioUploadValidator.ValidatedAudio audio = validator.validate(file);
        persistenceService.validateAnswerable(userId, diaryId, questionId);
        String key = storage.storeAnswer(diaryId, questionId, audio.bytes());
        DiaryAnswer answer;
        try {
            answer = persistenceService.createUploaded(userId, diaryId, questionId, key);
        } catch (RuntimeException exception) {
            storage.deleteQuietly(key);
            throw exception;
        }

        try {
            asyncService.transcribe(userId, diaryId, questionId, answer.getId());
            return new AnswerUploadResponse(answer.getId(), answer.getTranscriptionStatus());
        } catch (RuntimeException exception) {
            try {
                persistenceService.deleteUploaded(answer.getId());
                storage.deleteQuietly(key);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
                log.error("Failed to clean up rejected answer upload: answerId={}",
                        answer.getId(), cleanupException);
            }
            throw exception;
        }
    }
}
