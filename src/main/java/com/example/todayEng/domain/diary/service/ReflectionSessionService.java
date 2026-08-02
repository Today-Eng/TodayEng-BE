package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.client.ReflectionQuestionLlmClient;
import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand;
import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionLlmResponse;
import com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse;
import com.example.todayEng.domain.diary.dto.sse.DiarySsePayload;
import com.example.todayEng.domain.diary.sse.DiarySseEmitterManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReflectionSessionService {

    private final ReflectionQuestionPersistenceService persistenceService;
    private final ReflectionQuestionLlmClient llmClient;
    private final DiarySseEmitterManager emitterManager;

    public ReflectionSessionResponse create(Long userId, Long diaryId) {
        ReflectionQuestionGenerationCommand command =
                persistenceService.prepare(userId, diaryId);

        try {
            ReflectionQuestionLlmResponse llmResponse =
                    llmClient.generateQuestions(command);
            ReflectionSessionResponse response =
                    persistenceService.saveQuestions(command, llmResponse);

            emitterManager.sendQuestionsReady(
                    userId,
                    diaryId,
                    new DiarySsePayload.QuestionsReady(response.questions())
            );
            return response;
        } catch (RuntimeException exception) {
            persistenceService.markFailed(userId, diaryId);
            throw exception;
        }
    }
}
