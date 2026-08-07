package com.example.todayEng.domain.home.dto;

import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record DailyContextPreloadResponse(
        @Schema(description = "수집 대상 날짜", example = "2026-08-08")
        LocalDate contextDate,

        @Schema(description = "전체 수집을 건너뛴 사유. 수집을 수행했으면 null")
        SkipReason skipReason,

        @Schema(description = "컨텍스트 타입별 수집 결과")
        List<ContextResult> contexts
) {

    public static DailyContextPreloadResponse skipped(LocalDate contextDate, SkipReason skipReason) {
        return new DailyContextPreloadResponse(contextDate, skipReason, List.of());
    }

    public static DailyContextPreloadResponse collected(
            LocalDate contextDate,
            List<ContextResult> contexts
    ) {
        return new DailyContextPreloadResponse(contextDate, null, List.copyOf(contexts));
    }

    public record ContextResult(
            @Schema(description = "컨텍스트 타입", example = "WEATHER")
            DiaryContextType type,

            @Schema(description = "수집 결과", example = "SUCCEEDED")
            ResultStatus status
    ) {
    }

    public enum SkipReason {
        DIARY_ALREADY_STARTED
    }

    public enum ResultStatus {
        SUCCEEDED,
        FAILED,
        NO_LOCATION,
        ALREADY_IN_PROGRESS
    }
}
