package com.example.todayEng.domain.diary.client;

import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand;
import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionLlmResponse;

public interface ReflectionQuestionLlmClient {

    ReflectionQuestionLlmResponse generateQuestions(
            ReflectionQuestionGenerationCommand command
    );
}
