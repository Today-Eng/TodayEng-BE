package com.example.todayEng.domain.diary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "WAITING: 비동기 처리 대기, QUESTION_READY: 답변 가능, READY_TO_COMPLETE: 최종 저장 가능")
public enum NextQuestionStatus {
    WAITING,
    QUESTION_READY,
    READY_TO_COMPLETE
}
