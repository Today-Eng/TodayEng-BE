package com.example.todayEng.domain.diary.dto.tts;

public record QuestionTtsCommand(
        Long userId,
        Long diaryId,
        Long questionId,
        String questionText
) {
}
