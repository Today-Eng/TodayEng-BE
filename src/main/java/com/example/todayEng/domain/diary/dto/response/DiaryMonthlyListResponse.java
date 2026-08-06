package com.example.todayEng.domain.diary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record DiaryMonthlyListResponse(

        @Schema(description = "조회 연도", example = "2026")
        int year,

        @Schema(description = "조회 월", example = "7")
        int month,

        @Schema(description = "월별 회고 목록")
        List<DiarySummary> diaries

) {

    public static DiaryMonthlyListResponse of(
            int year,
            int month,
            List<DiarySummary> diaries
    ) {
        return new DiaryMonthlyListResponse(
                year,
                month,
                diaries
        );
    }

    public static DiaryMonthlyListResponse empty(
            int year,
            int month
    ) {
        return new DiaryMonthlyListResponse(
                year,
                month,
                List.of()
        );
    }

    public record DiarySummary(

            @Schema(description = "회고 ID", example = "4521")
            Long diaryId,

            @Schema(description = "회고 날짜", example = "2026-07-26")
            LocalDate diaryDate,

            @Schema(description = "요일", example = "SUNDAY")
            DayOfWeek dayOfWeek,

            @Schema(
                    description = "MAIN 질문의 키워드 목록",
                    example = "[\"날씨\", \"우산\", \"퇴근\"]"
            )
            List<String> keywords,

            @Schema(
                    description = "첫 번째 MAIN 질문",
                    example = "What did you do today?"
            )
            String questionText,

            @Schema(
                    description = "첫 번째 MAIN 질문 답변의 교정문",
                    example = "I visited a museum and studied English."
            )
            String correctedText
    ) {

        public static DiarySummary of(
                Long diaryId,
                LocalDate diaryDate,
                List<String> keywords,
                String questionText,
                String correctedText
        ) {
            return new DiarySummary(
                    diaryId,
                    diaryDate,
                    diaryDate.getDayOfWeek(),
                    keywords,
                    questionText,
                    correctedText
            );
        }
    }
}
