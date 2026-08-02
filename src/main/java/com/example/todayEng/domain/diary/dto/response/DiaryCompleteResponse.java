package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import java.time.LocalDateTime;

public record DiaryCompleteResponse(Long diaryId, DiaryStatus status, String finalMemo, LocalDateTime completedAt) {
    public static DiaryCompleteResponse from(Diary diary) {
        return new DiaryCompleteResponse(diary.getId(), diary.getStatus(), diary.getMemo(), diary.getCompletedAt());
    }
}
