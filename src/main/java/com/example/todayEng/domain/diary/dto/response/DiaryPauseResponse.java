package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record DiaryPauseResponse(
        @Schema(description = "회고 ID", example = "1") Long diaryId,
        @Schema(description = "회고 상태", example = "COMPLETED") DiaryStatus status,
        @Schema(description = "완료 일시", example = "2026-08-03T22:30:00") LocalDateTime completedAt
) {
    public static DiaryPauseResponse from(Diary diary) {
        return new DiaryPauseResponse(
                diary.getId(),
                diary.getStatus(),
                diary.getCompletedAt()
        );
    }
}
