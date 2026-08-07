package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiaryStartResponse(
        @Schema(description = "회고 ID", example = "1")
        Long diaryId,

        @Schema(description = "회고 날짜", example = "2026-07-28")
        LocalDate diaryDate,

        @Schema(description = "회고 상태", example = "IN_PROGRESS")
        DiaryStatus status,

        @Schema(description = "재개 여부", example = "false")
        boolean resumed,

        @Schema(description = "생성 일시", example = "2026-07-28T09:00:00")
        LocalDateTime createdAt
) {

    public static DiaryStartResponse from(Diary diary) {
        return from(diary, false);
    }

    public static DiaryStartResponse from(Diary diary, boolean resumed) {
        return new DiaryStartResponse(
                diary.getId(),
                diary.getDiaryDate(),
                diary.getStatus(),
                resumed,
                diary.getCreatedAt()
        );
    }
}
