package com.example.todayEng.domain.diary.dto.llm;

import java.util.List;

public record AnswerCorrectionLlmResponse(
        String correctedText,
        String correctionReason,
        List<String> alternativeExpressions,
        FollowUpQuestion followUpQuestion
) {
    public record FollowUpQuestion(String questionText, String koreanTranslation) { }
}
