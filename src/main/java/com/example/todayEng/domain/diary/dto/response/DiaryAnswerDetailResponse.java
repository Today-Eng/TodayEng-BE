package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.entity.enums.CorrectionStatus;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.diary.entity.enums.TranscriptionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record DiaryAnswerDetailResponse(
        Long answerId, Long questionId, QuestionType questionType, Integer questionOrder,
        String questionText, String koreanTranslation, String originalText, String correctedText,
        String correctionReason, JsonNode alternativeExpression, CorrectionStatus correctionStatus,
        TranscriptionStatus transcriptionStatus, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static DiaryAnswerDetailResponse from(DiaryAnswer answer) {
        var q = answer.getQuestion();
        return new DiaryAnswerDetailResponse(answer.getId(), q.getId(), q.getQuestionType(), q.getQuestionOrder(),
                q.getQuestionText(), q.getKoreanTranslation(), answer.getOriginalText(), answer.getCorrectedText(),
                answer.getCorrectionReason(), answer.getAlternativeExpression(), answer.getCorrectionStatus(),
                answer.getTranscriptionStatus(), answer.getCreatedAt(), answer.getUpdatedAt());
    }
}
