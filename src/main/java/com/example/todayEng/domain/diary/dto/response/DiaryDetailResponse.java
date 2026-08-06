package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record DiaryDetailResponse(

        @Schema(description = "회고 ID", example = "4521")
        Long diaryId,

        @Schema(description = "회고 날짜", example = "2026-07-10")
        LocalDate diaryDate,

        @Schema(description = "요일", example = "FRIDAY")
        DayOfWeek dayOfWeek,

        @Schema(
                description = "MAIN 질문 키워드 목록",
                example = "[\"날씨\", \"우산\", \"퇴근\"]"
        )
        List<String> keywords,

        @Schema(description = "질문 및 답변 목록")
        List<QuestionAnswer> qaList,

        @Schema(
                description = "회고 메모",
                example = "오늘은 친구들을 만나 기분이 정말 좋았다.",
                nullable = true
        )
        String memo

) {

    public static DiaryDetailResponse of(
            Long diaryId,
            LocalDate diaryDate,
            List<String> keywords,
            List<QuestionAnswer> qaList,
            String memo
    ) {
        return new DiaryDetailResponse(
                diaryId,
                diaryDate,
                diaryDate.getDayOfWeek(),
                keywords,
                qaList,
                memo
        );
    }

    public record QuestionAnswer(

            @Schema(description = "질문 ID", example = "3")
            Long questionId,

            @Schema(description = "질문 순서", example = "1")
            Integer questionOrder,

            @Schema(description = "질문 유형", example = "MAIN")
            QuestionType questionType,

            @Schema(
                    description = "영어 질문",
                    example = "How did you feel?"
            )
            String questionText,

            @Schema(
                    description = "질문 한국어 번역",
                    example = "오늘 기분이 어땠나요?"
            )
            String questionKoreanTranslation,

            @Schema(
                    description = "MAIN 질문 키워드",
                    example = "감정",
                    nullable = true
            )
            String keyword,

            @Schema(
                    description = "질문에 대한 답변",
                    nullable = true
            )
            Answer answer

    ) {

        public static QuestionAnswer of(
                Long questionId,
                Integer questionOrder,
                QuestionType questionType,
                String questionText,
                String questionKoreanTranslation,
                String keyword,
                Answer answer
        ) {
            return new QuestionAnswer(
                    questionId,
                    questionOrder,
                    questionType,
                    questionText,
                    questionKoreanTranslation,
                    keyword,
                    answer
            );
        }
    }

    public record Answer(

            @Schema(
                    description = "사용자가 작성한 원문",
                    example = "I was very happy."
            )
            String originalText,

            @Schema(
                    description = "교정된 문장",
                    example = "I felt very happy.",
                    nullable = true
            )
            String correctedText,

            @Schema(
                    description = "교정 이유",
                    example = "felt가 더 자연스러운 표현입니다.",
                    nullable = true
            )
            String correctionReason,

            @Schema(
                    description = "대체 표현",
                    nullable = true
            )
            JsonNode alternativeExpression

    ) {

        public static Answer of(
                String originalText,
                String correctedText,
                String correctionReason,
                JsonNode alternativeExpression
        ) {
            return new Answer(
                    originalText,
                    correctedText,
                    correctionReason,
                    alternativeExpression
            );
        }
    }
}
