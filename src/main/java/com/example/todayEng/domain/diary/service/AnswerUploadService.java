package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.response.AnswerUploadResponse;
import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.storage.AudioFileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        try {
            DiaryAnswer answer = persistenceService.createUploaded(userId, diaryId, questionId, key);
            asyncService.transcribe(userId, diaryId, questionId, answer.getId());
            return new AnswerUploadResponse(answer.getId(), answer.getTranscriptionStatus());
        } catch (RuntimeException exception) {
            storage.deleteQuietly(key);
            throw exception;
        }
    }
}
