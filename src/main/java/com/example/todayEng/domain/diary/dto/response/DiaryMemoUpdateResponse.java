package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.Diary;
import io.swagger.v3.oas.annotations.media.Schema;

public record DiaryMemoUpdateResponse(
        @Schema(description = "회고 ID", example = "1")
        Long diaryId,
        @Schema(
                description = "수정된 메모",
                example = "오늘 공연이 너무 재밌었다.",
                nullable = true
        )
        String memo
) {
    public static DiaryMemoUpdateResponse from(Diary diary) {
        return new DiaryMemoUpdateResponse(
                diary.getId(),
                diary.getMemo()
        );
    }
}
