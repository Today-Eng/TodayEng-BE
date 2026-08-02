package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.CorrectionStatus;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.diary.entity.enums.TranscriptionStatus;
import com.example.todayEng.domain.diary.entity.enums.TtsStatus;

public record DiaryQuestionResponse(
        Long questionId,
        Long parentQuestionId,
        QuestionType questionType,
        Integer questionOrder,
        String questionText,
        String koreanTranslation,
        TtsStatus ttsStatus,
        String ttsAudioUrl,
        Long answerId,
        TranscriptionStatus transcriptionStatus,
        CorrectionStatus correctionStatus
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
