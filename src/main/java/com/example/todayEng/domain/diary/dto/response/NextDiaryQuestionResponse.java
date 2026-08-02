package com.example.todayEng.domain.diary.dto.response;

public record NextDiaryQuestionResponse(
        NextQuestionStatus status,
        DiaryQuestionResponse question
) { }
