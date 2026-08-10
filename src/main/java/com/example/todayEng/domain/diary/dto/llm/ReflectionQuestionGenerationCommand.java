package com.example.todayEng.domain.diary.dto.llm;

import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record ReflectionQuestionGenerationCommand(
        Long userId,
        Long diaryId,
        long leaseVersion,
        String nickname,
        EnglishLevel englishLevel,
        List<String> interests,
        List<ContextInput> contexts
) {

    public static final int TOTAL_QUESTIONS = 3;

    public ReflectionQuestionGenerationCommand {
        nickname = nickname == null ? "" : nickname;
        interests = interests == null ? List.of() : List.copyOf(interests);
        contexts = contexts == null ? List.of() : List.copyOf(contexts);
    }

    /**
     * 성공한 컨텍스트 수에 따라 컨텍스트 기반 질문과 관심사 기반 질문의 개수를 정한다.
     *
     * <p>프롬프트와 응답 검증이 서로 다른 개수를 기준으로 삼으면 LLM에 요구한 구성과
     * 검증 기준이 어긋나므로 한 곳에서만 계산한다.
     */
    public QuestionPlan plan() {
        int contextQuestions;
        if (contexts.isEmpty()) {
            contextQuestions = 0;
        } else if (interests.isEmpty()) {
            // 관심사가 없으면 대체할 소재가 없으므로 컨텍스트 반복을 허용한다
            contextQuestions = TOTAL_QUESTIONS;
        } else {
            contextQuestions = Math.min(contexts.size(), TOTAL_QUESTIONS);
        }

        return new QuestionPlan(
                contextQuestions,
                TOTAL_QUESTIONS - contextQuestions,
                contextQuestions <= contexts.size()
        );
    }

    public record QuestionPlan(
            int contextQuestionCount,
            int interestQuestionCount,
            boolean requireDistinctContexts
    ) {
    }

    public record ContextInput(
            Long contextId,
            DiaryContextType contextType,
            JsonNode contextData
    ) {
    }
}
