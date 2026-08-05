package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record DiaryPauseResponse(
        @Schema(description = "회고 ID", example = "1") Long diaryId,
        @Schema(description = "회고 상태", example = "PAUSED") DiaryStatus status,
        @Schema(description = "중단 일시", example = "2026-08-03T22:30:00") LocalDateTime pausedAt,
        @Schema(description = "재개 만료 일시", example = "2026-08-04T22:30:00") LocalDateTime expiresAt
) {
    public static DiaryPauseResponse from(Diary diary) {
        return new DiaryPauseResponse(
                diary.getId(),
                diary.getStatus(),
                diary.getPausedAt(),
                diary.getPauseExpiresAt()
        );
    }
}
