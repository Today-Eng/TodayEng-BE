package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.client.AnswerCorrectionLlmClient;
import com.example.todayEng.domain.diary.dto.llm.AnswerCorrectionResult;
import com.example.todayEng.domain.diary.dto.llm.AnswerCorrectionWork;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
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
        AnswerCorrectionResult result;
        try {
            work = persistenceService.claim(userId, diaryId, questionId, answerId);
            var llmResponse = llmClient.correct(work.command());
            result = persistenceService.complete(work, llmResponse);
        } catch (RuntimeException exception) {
            if (canUseDefaultFollowUp(work, exception)) {
                try {
                    result = persistenceService.completeWithDefaultFollowUp(work);
                    log.warn("AI follow-up generation failed; used a default follow-up: diaryId={}, questionId={}",
                            diaryId, questionId, exception);
                } catch (RuntimeException fallbackException) {
                    fallbackException.addSuppressed(exception);
                    handleProcessingFailure(userId, diaryId, questionId, answerId, work, fallbackException);
                    return;
                }
            } else {
                handleProcessingFailure(userId, diaryId, questionId, answerId, work, exception);
                return;
            }
        }

        AnswerCorrectionResult completedResult = result;
        runFollowUpAction("answer-corrected notification", diaryId, questionId, answerId,
                () -> emitterManager.sendAnswerCorrected(userId, diaryId, new DiarySsePayload.AnswerCorrected(
                        questionId, answerId, completedResult.correctedText(), completedResult.correctionReason())));
        runFollowUpAction("next step", diaryId, questionId, answerId, () -> {
            if (completedResult.nextQuestion() != null) {
                if (completedResult.nextQuestion().followUp()) {
                    questionTtsService.generateFollowUpQuestion(userId, diaryId, completedResult.nextQuestion().id(),
                            completedResult.nextQuestion().koreanTranslation());
                } else {
                    questionTtsService.generateQuestion(userId, diaryId, completedResult.nextQuestion().id(),
                            completedResult.nextQuestion().koreanTranslation());
                }
            } else if (completedResult.readyToComplete()) {
                emitterManager.sendReadyToComplete(userId, diaryId);
            }
        });
    }

    private boolean canUseDefaultFollowUp(AnswerCorrectionWork work, RuntimeException exception) {
        if (work == null || work.command().questionType() != QuestionType.MAIN) {
            return false;
        }
        if (!(exception instanceof BaseException baseException)) {
            return false;
        }
        return baseException.getErrorCode() == ErrorCode.LLM_API_FAILED
                || baseException.getErrorCode() == ErrorCode.INVALID_LLM_RESPONSE;
    }

    private void runFollowUpAction(String action, Long diaryId, Long questionId, Long answerId, Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException exception) {
            log.error("Answer correction {} failed: diaryId={}, questionId={}, answerId={}",
                    action, diaryId, questionId, answerId, exception);
        }
    }

    private void handleProcessingFailure(Long userId, Long diaryId, Long questionId, Long answerId,
            AnswerCorrectionWork work, RuntimeException exception) {
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

    private String errorCode(RuntimeException exception) {
        return exception instanceof BaseException baseException
                ? baseException.getErrorCode().getCode() : "INTERNAL_ERROR";
    }
}
