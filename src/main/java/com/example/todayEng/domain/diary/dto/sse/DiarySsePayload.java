package com.example.todayEng.domain.diary.dto.sse;

import com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse;

public final class DiarySsePayload {

    private DiarySsePayload() {
    }

    public record Connected(String message) {
    }

    public record Heartbeat(String message) {
    }

    public record QuestionReady(
            Long questionId,
            String questionText,
            String koreanTranslation,
            String audioUrl
    ) {
    }

    public record QuestionsReady(
            java.util.List<ReflectionSessionResponse.Question> questions
    ) {
    }

    public record AnswerTranscribed(
            Long questionId,
            Long answerId,
            String originalText
    ) {
    }

    public record AnswerCorrected(
            Long questionId,
            Long answerId,
            String correctedText,
            String correctionReason
    ) {
    }

    public record ProcessingFailed(
            String stage,
            String errorCode,
            String message
    ) {
    }
}
