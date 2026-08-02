package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일기 최종 저장 결과")
public record DiaryCompleteResponse(
        @Schema(example = "1") Long diaryId,
        @Schema(example = "COMPLETED", allowableValues = {"COMPLETED"}) DiaryStatus status,
        @Schema(description = "DB Diary.memo에 저장되는 최종 메모", example = "오늘의 최종 회고", nullable = true) String finalMemo,
        @Schema(example = "2026-08-02T16:30:00") LocalDateTime completedAt) {
    public static DiaryCompleteResponse from(Diary diary) {
        return new DiaryCompleteResponse(diary.getId(), diary.getStatus(), diary.getMemo(), diary.getCompletedAt());
    }
}
