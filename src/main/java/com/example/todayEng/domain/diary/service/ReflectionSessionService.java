package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.client.ReflectionQuestionLlmClient;
import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand;
import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionLlmResponse;
import com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse;
import com.example.todayEng.domain.diary.dto.sse.DiarySsePayload;
import com.example.todayEng.domain.diary.sse.DiarySseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReflectionSessionService {

    private final ReflectionQuestionPersistenceService persistenceService;
    private final ReflectionQuestionLlmClient llmClient;
    private final DiarySseEmitterManager emitterManager;
    private final QuestionTtsService questionTtsService;

    public ReflectionSessionResponse create(Long userId, Long diaryId) {
        ReflectionQuestionGenerationCommand command =
                persistenceService.prepare(userId, diaryId);

        ReflectionSessionResponse response;
        try {
            ReflectionQuestionLlmResponse llmResponse =
                    llmClient.generateQuestions(command);
            response = persistenceService.saveQuestions(command, llmResponse);
        } catch (RuntimeException exception) {
            log.warn("AI question generation failed; trying default questions: userId={}, diaryId={}",
                    userId, diaryId, exception);
            try {
                response = persistenceService.saveDefaultQuestions(command);
            } catch (RuntimeException fallbackException) {
                persistenceService.markFailed(command);
                fallbackException.addSuppressed(exception);
                throw fallbackException;
            }
        }
        try {
            emitterManager.sendQuestionsReady(
                    userId,
                    diaryId,
                    new DiarySsePayload.QuestionsReady(response.questions())
            );
        } catch (RuntimeException exception) {
            log.error("Questions-ready notification failed: userId={}, diaryId={}",
                    userId, diaryId, exception);
        }
        try {
            questionTtsService.generateFirstQuestion(userId, response);
        } catch (RuntimeException exception) {
            log.error("First-question TTS request failed: userId={}, diaryId={}",
                    userId, diaryId, exception);
        }
        return response;
    }
}
