package com.example.todayEng.domain.diary.dto.llm;

import java.time.LocalDate;
import java.util.List;

public record DiaryMemoryAnalysisCommand(
        Long currentDiaryId,
        List<DiaryInput> diaries
) {

    public record DiaryInput(
            Long diaryId,
            LocalDate diaryDate,
            String memo,
            List<ReflectionInput> reflections
    ) {
    }

    public record ReflectionInput(
            String question,
            String answer
    ) {
    }
}
