package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.client.AnswerCorrectionLlmClient;
import com.example.todayEng.domain.diary.dto.llm.AnswerCorrectionResult;
import com.example.todayEng.domain.diary.dto.llm.AnswerCorrectionWork;
import com.example.todayEng.domain.diary.dto.sse.DiarySsePayload;
import com.example.todayEng.domain.diary.sse.DiarySseEmitterManager;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerCorrectionPipelineService {
    private final AnswerCorrectionPersistenceService persistenceService;
    private final AnswerCorrectionLlmClient llmClient;
    private final DiarySseEmitterManager emitterManager;
    private final QuestionTtsService questionTtsService;

    public void process(Long userId, Long diaryId, Long questionId, Long answerId) {
        AnswerCorrectionWork work = null;
        try {
            work = persistenceService.claim(userId, diaryId, questionId, answerId);
            var llmResponse = llmClient.correct(work.command());
            AnswerCorrectionResult result = persistenceService.complete(work, llmResponse);
            emitterManager.sendAnswerCorrected(userId, diaryId, new DiarySsePayload.AnswerCorrected(
                    questionId, answerId, result.correctedText(), result.correctionReason()));
            if (result.nextQuestion() != null) {
                if (result.nextQuestion().followUp()) {
                    questionTtsService.generateFollowUpQuestion(userId, diaryId, result.nextQuestion().id(),
                            result.nextQuestion().koreanTranslation());
                } else {
                    questionTtsService.generateQuestion(userId, diaryId, result.nextQuestion().id(),
                            result.nextQuestion().koreanTranslation());
                }
            } else if (result.readyToComplete()) {
                emitterManager.sendReadyToComplete(userId, diaryId);
            }
        } catch (RuntimeException exception) {
            if (exception instanceof BaseException baseException
                    && baseException.getErrorCode() == ErrorCode.ANSWER_CORRECTION_ALREADY_PROCESSING) {
                log.debug("Answer correction was already claimed: answerId={}", answerId);
                return;
            }
            if (work != null) persistenceService.fail(answerId, exception);
            emitterManager.sendProcessingFailed(userId, diaryId,
                    new DiarySsePayload.ProcessingFailed("ANSWER_CORRECTION", errorCode(exception),
                            "답변 교정 처리에 실패했습니다."));
            log.error("Answer correction failed: diaryId={}, questionId={}, answerId={}",
                    diaryId, questionId, answerId, exception);
        }
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof BaseException baseException
                ? baseException.getErrorCode().getCode() : "INTERNAL_ERROR";
    }
}
