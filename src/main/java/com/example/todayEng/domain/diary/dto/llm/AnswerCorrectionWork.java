package com.example.todayEng.domain.diary.dto.llm;

public record AnswerCorrectionWork(
        Long userId,
        Long diaryId,
        Long questionId,
        Long answerId,
        AnswerCorrectionCommand command
) { }
