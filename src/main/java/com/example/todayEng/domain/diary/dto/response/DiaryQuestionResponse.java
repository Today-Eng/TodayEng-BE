package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.CorrectionStatus;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.diary.entity.enums.TranscriptionStatus;
import com.example.todayEng.domain.diary.entity.enums.TtsStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "질문 및 비동기 처리 진행 상태")
public record DiaryQuestionResponse(
        @Schema(example = "10") Long questionId,
        @Schema(description = "FOLLOW_UP의 부모 MAIN 질문 ID", example = "9", nullable = true) Long parentQuestionId,
        @Schema(example = "MAIN", allowableValues = {"MAIN", "FOLLOW_UP"}) QuestionType questionType,
        @Schema(description = "전체 진행 순서 1~6", example = "1") Integer questionOrder,
        @Schema(example = "What was the most memorable part of your day?") String questionText,
        @Schema(example = "오늘 가장 기억에 남는 일은 무엇인가요?") String koreanTranslation,
        @Schema(example = "SUCCEEDED", allowableValues = {"PENDING", "PROCESSING", "SUCCEEDED", "FAILED"}) TtsStatus ttsStatus,
        @Schema(description = "TTS 성공 시 재생 URL", example = "/files/audio/diaries/1/questions/10/audio.mp3", nullable = true) String ttsAudioUrl,
        @Schema(description = "답변이 있을 때만 반환", example = "20", nullable = true) Long answerId,
        @Schema(nullable = true, allowableValues = {"UPLOADED", "PROCESSING", "SUCCEEDED", "FAILED"}) TranscriptionStatus transcriptionStatus,
        @Schema(nullable = true, allowableValues = {"PENDING", "PROCESSING", "SUCCEEDED", "FAILED"}) CorrectionStatus correctionStatus
) {
    public static DiaryQuestionResponse from(
            DiaryQuestion question,
            DiaryAnswer answer,
            String audioUrl
    ) {
        return new DiaryQuestionResponse(
                question.getId(),
                question.getParentQuestion() == null ? null : question.getParentQuestion().getId(),
                question.getQuestionType(),
                question.getQuestionOrder(),
                question.getQuestionText(),
                question.getKoreanTranslation(),
                question.getTtsStatus(),
                audioUrl,
                answer == null ? null : answer.getId(),
                answer == null ? null : answer.getTranscriptionStatus(),
                answer == null ? null : answer.getCorrectionStatus()
        );
    }
}
