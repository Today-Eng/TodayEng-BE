package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import com.example.todayEng.domain.diary.client.ReflectionQuestionLlmClient;
import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand;
import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionLlmResponse;
import com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse;
import com.example.todayEng.domain.diary.dto.sse.DiarySsePayload;
import com.example.todayEng.domain.diary.sse.DiarySseEmitterManager;
import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReflectionSessionServiceTest {

    @Mock
    private ReflectionQuestionPersistenceService persistenceService;
    @Mock
    private ReflectionQuestionLlmClient llmClient;
    @Mock
    private DiarySseEmitterManager emitterManager;
    @Mock
    private QuestionTtsService questionTtsService;
    @InjectMocks
    private ReflectionSessionService service;

    @Test
    void callsLlmOnceAndPublishesQuestionsReady() {
        var command = new ReflectionQuestionGenerationCommand(
                1L, 10L, 1L, EnglishLevel.BEGINNER, List.of(), List.of()
        );
        var llmResponse = new ReflectionQuestionLlmResponse(List.of());
        var response = new ReflectionSessionResponse(10L, List.of());
        given(persistenceService.prepare(1L, 10L)).willReturn(command);
        given(llmClient.generateQuestions(command)).willReturn(llmResponse);
        given(persistenceService.saveQuestions(command, llmResponse))
                .willReturn(response);

        ReflectionSessionResponse result = service.create(1L, 10L);

        assertThat(result).isSameAs(response);
        verify(llmClient).generateQuestions(command);
        verify(emitterManager).sendQuestionsReady(
                1L,
                10L,
                new DiarySsePayload.QuestionsReady(response.questions())
        );
        verify(questionTtsService).generateFirstQuestion(1L, response);
    }

    @Test
    void marksGenerationFailedWhenLlmFails() {
        var command = new ReflectionQuestionGenerationCommand(
                1L, 10L, 1L, EnglishLevel.BEGINNER, List.of(), List.of()
        );
        RuntimeException failure = new RuntimeException("llm failed");
        given(persistenceService.prepare(1L, 10L)).willReturn(command);
        given(llmClient.generateQuestions(command)).willThrow(failure);

        assertThatThrownBy(() -> service.create(1L, 10L))
                .isSameAs(failure);
        verify(persistenceService).markFailed(command);
    }

    @Test
    void notificationFailureDoesNotMarkSavedQuestionsFailed() {
        var command = new ReflectionQuestionGenerationCommand(
                1L, 10L, 1L, EnglishLevel.BEGINNER, List.of(), List.of());
        var llmResponse = new ReflectionQuestionLlmResponse(List.of());
        var response = new ReflectionSessionResponse(10L, List.of());
        given(persistenceService.prepare(1L, 10L)).willReturn(command);
        given(llmClient.generateQuestions(command)).willReturn(llmResponse);
        given(persistenceService.saveQuestions(command, llmResponse)).willReturn(response);
        doThrow(new RuntimeException("disconnected")).when(emitterManager)
                .sendQuestionsReady(eq(1L), eq(10L), any());

        assertThat(service.create(1L, 10L)).isSameAs(response);

        verify(persistenceService, never()).markFailed(command);
        verify(questionTtsService).generateFirstQuestion(1L, response);
    }
}
