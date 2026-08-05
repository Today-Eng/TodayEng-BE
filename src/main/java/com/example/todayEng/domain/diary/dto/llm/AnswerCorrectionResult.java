package com.example.todayEng.domain.diary.dto.llm;

import com.example.todayEng.domain.diary.entity.enums.QuestionType;

public record AnswerCorrectionResult(
        QuestionType answeredQuestionType,
        Long questionId,
        Long answerId,
        String correctedText,
        String correctionReason,
        NextQuestion nextQuestion,
        boolean readyToComplete
) {
    public record NextQuestion(Long id, String koreanTranslation, boolean followUp) { }
}
