package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.enums.TranscriptionStatus;

public record AnswerUploadResponse(Long answerId, TranscriptionStatus status) {
}
