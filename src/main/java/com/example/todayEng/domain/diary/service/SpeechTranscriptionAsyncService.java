package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.client.GoogleSpeechToTextClient;
import com.example.todayEng.domain.diary.dto.sse.DiarySsePayload;
import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.sse.DiarySseEmitterManager;
import com.example.todayEng.domain.diary.storage.AudioFileStorage;
import com.example.todayEng.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechTranscriptionAsyncService {
    private final AnswerPersistenceService persistenceService;
    private final AudioFileStorage audioFileStorage;
    private final GoogleSpeechToTextClient speechClient;
    private final DiarySseEmitterManager emitterManager;
    private final AnswerCorrectionPipelineService correctionPipelineService;

    @Async("speechTaskExecutor")
    public void transcribe(Long userId, Long diaryId, Long questionId, Long answerId) {
        String audioKey;
        String text;
        try {
            if (!persistenceService.claim(answerId)) return;
            DiaryAnswer answer = persistenceService.getOwned(userId, diaryId, questionId, answerId);
            audioKey = answer.getAudioKey();
            text = speechClient.transcribe(audioFileStorage.read(audioKey));
            persistenceService.complete(answerId, text);
        } catch (RuntimeException exception) {
            persistenceService.fail(answerId, exception.getMessage());
            try {
                emitterManager.sendProcessingFailed(userId, diaryId,
                        new DiarySsePayload.ProcessingFailed("TRANSCRIPTION", errorCode(exception),
                                "음성 인식에 실패했습니다. 다시 녹음해 주세요."));
            } catch (RuntimeException notificationException) {
                log.error("STT failure notification failed: diaryId={}, questionId={}, answerId={}",
                        diaryId, questionId, answerId, notificationException);
            }
            log.error("STT processing failed: diaryId={}, questionId={}, answerId={}",
                    diaryId, questionId, answerId, exception);
            return;
        }

        audioFileStorage.deleteQuietly(audioKey);
        try {
            emitterManager.sendAnswerTranscribed(userId, diaryId,
                    new DiarySsePayload.AnswerTranscribed(questionId, answerId, text));
        } catch (RuntimeException exception) {
            log.error("STT notification failed: diaryId={}, questionId={}, answerId={}",
                    diaryId, questionId, answerId, exception);
        }
        try {
            correctionPipelineService.process(userId, diaryId, questionId, answerId);
        } catch (RuntimeException exception) {
            log.error("Unable to start answer correction: diaryId={}, questionId={}, answerId={}",
                    diaryId, questionId, answerId, exception);
        }
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof BaseException baseException
                ? baseException.getErrorCode().getCode() : "INTERNAL_ERROR";
    }
}
