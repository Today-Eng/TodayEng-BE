package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import java.util.List;

public record DiaryContextCreateResponse(
        Long diaryId,
        List<ContextResult> contexts
) {
    public record ContextResult(
            DiaryContextType type,
            boolean success
    ) {
        public static ContextResult from(DiaryContext context) {
            return new ContextResult(context.getContextType(), context.isSuccess());
        }
    }
}
