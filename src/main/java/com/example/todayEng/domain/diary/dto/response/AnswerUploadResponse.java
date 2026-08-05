package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.enums.TranscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "음성 답변 접수 결과")
public record AnswerUploadResponse(
        @Schema(description = "생성된 답변 ID", example = "20") Long answerId,
        @Schema(description = "접수 시점 STT 상태", example = "UPLOADED", allowableValues = {"UPLOADED"}) TranscriptionStatus status) {
}
