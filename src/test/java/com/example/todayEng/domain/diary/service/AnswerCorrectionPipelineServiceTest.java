package com.example.todayEng.domain.diary.service;

import static org.mockito.Mockito.*;

import com.example.todayEng.domain.diary.client.AnswerCorrectionLlmClient;
import com.example.todayEng.domain.diary.dto.llm.*;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.diary.sse.DiarySseEmitterManager;
import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnswerCorrectionPipelineServiceTest {
    @Mock AnswerCorrectionPersistenceService persistenceService;
    @Mock AnswerCorrectionLlmClient llmClient;
    @Mock DiarySseEmitterManager emitterManager;
    @Mock QuestionTtsService ttsService;
    @InjectMocks AnswerCorrectionPipelineService service;

    @Test
    void mainAnswerUsesOneLlmCallThenCreatesFollowUpTtsFlow() {
        var command = new AnswerCorrectionCommand(QuestionType.MAIN, "q", "a", EnglishLevel.BEGINNER, null);
        var work = new AnswerCorrectionWork(1L, 2L, 3L, 4L, command);
        var llm = new AnswerCorrectionLlmResponse("correct", "reason", List.of("alt"),
                new AnswerCorrectionLlmResponse.FollowUpQuestion("follow?", "후속?"));
        var result = new AnswerCorrectionResult(QuestionType.MAIN, 3L, 4L, "correct", "reason",
                new AnswerCorrectionResult.NextQuestion(5L, "후속?", true), false);
        when(persistenceService.claim(1L, 2L, 3L, 4L)).thenReturn(work);
        when(llmClient.correct(command)).thenReturn(llm);
        when(persistenceService.complete(work, llm)).thenReturn(result);

        service.process(1L, 2L, 3L, 4L);

        verify(llmClient, times(1)).correct(command);
        verify(emitterManager).sendAnswerCorrected(eq(1L), eq(2L), any());
        verify(ttsService).generateFollowUpQuestion(1L, 2L, 5L, "후속?");
    }

    @Test
    void finalFollowUpSendsReadyToComplete() {
        var command = new AnswerCorrectionCommand(QuestionType.FOLLOW_UP, "q", "a", EnglishLevel.ADVANCED, null);
        var work = new AnswerCorrectionWork(1L, 2L, 3L, 4L, command);
        var llm = new AnswerCorrectionLlmResponse("correct", "reason", List.of(), null);
        var result = new AnswerCorrectionResult(QuestionType.FOLLOW_UP, 3L, 4L, "correct", "reason", null, true);
        when(persistenceService.claim(1L, 2L, 3L, 4L)).thenReturn(work);
        when(llmClient.correct(command)).thenReturn(llm);
        when(persistenceService.complete(work, llm)).thenReturn(result);

        service.process(1L, 2L, 3L, 4L);

        verify(emitterManager).sendReadyToComplete(1L, 2L);
        verifyNoInteractions(ttsService);
    }

    @Test
    void failedLlmIsPersistedForRetry() {
        var command = new AnswerCorrectionCommand(QuestionType.MAIN, "q", "a", EnglishLevel.BEGINNER, null);
        var work = new AnswerCorrectionWork(1L, 2L, 3L, 4L, command);
        when(persistenceService.claim(1L, 2L, 3L, 4L)).thenReturn(work);
        when(llmClient.correct(command)).thenThrow(new BaseException(ErrorCode.LLM_API_FAILED));

        service.process(1L, 2L, 3L, 4L);

        verify(persistenceService).fail(eq(4L), any());
        verify(emitterManager).sendProcessingFailed(eq(1L), eq(2L), any());
    }

    @Test
    void alreadyClaimedAnswerDoesNotCallLlm() {
        when(persistenceService.claim(1L, 2L, 3L, 4L))
                .thenThrow(new BaseException(ErrorCode.ANSWER_CORRECTION_ALREADY_PROCESSING));
        service.process(1L, 2L, 3L, 4L);
        verifyNoInteractions(llmClient);
    }

    @Test
    void notificationFailureDoesNotRevertCompletedCorrection() {
        var command = new AnswerCorrectionCommand(QuestionType.FOLLOW_UP, "q", "a", EnglishLevel.BEGINNER, null);
        var work = new AnswerCorrectionWork(1L, 2L, 3L, 4L, command);
        var llm = new AnswerCorrectionLlmResponse("correct", "reason", List.of(), null);
        var result = new AnswerCorrectionResult(QuestionType.FOLLOW_UP, 3L, 4L,
                "correct", "reason", null, true);
        when(persistenceService.claim(1L, 2L, 3L, 4L)).thenReturn(work);
        when(llmClient.correct(command)).thenReturn(llm);
        when(persistenceService.complete(work, llm)).thenReturn(result);
        doThrow(new RuntimeException("disconnected")).when(emitterManager)
                .sendAnswerCorrected(eq(1L), eq(2L), any());

        service.process(1L, 2L, 3L, 4L);

        verify(persistenceService, never()).fail(anyLong(), any());
        verify(emitterManager, never()).sendProcessingFailed(eq(1L), eq(2L), any());
        verify(emitterManager).sendReadyToComplete(1L, 2L);
    }
}
