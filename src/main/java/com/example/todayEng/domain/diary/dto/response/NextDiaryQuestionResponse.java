package com.example.todayEng.domain.diary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record NextDiaryQuestionResponse(
        @Schema(example = "QUESTION_READY", allowableValues = {"WAITING", "QUESTION_READY", "READY_TO_COMPLETE"}) NextQuestionStatus status,
        @Schema(description = "QUESTION_READY일 때만 반환", nullable = true) DiaryQuestionResponse question
) { }
