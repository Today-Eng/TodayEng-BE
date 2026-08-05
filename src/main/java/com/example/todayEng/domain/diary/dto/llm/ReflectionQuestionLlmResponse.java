package com.example.todayEng.domain.diary.dto.llm;

import java.util.List;

public record ReflectionQuestionLlmResponse(
        List<GeneratedQuestion> questions
) {

    public record GeneratedQuestion(
            Integer order,
            String questionText,
            String koreanTranslation,
            String keyword,
            Long contextId
    ) {
    }
}
