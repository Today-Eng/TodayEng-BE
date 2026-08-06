package com.example.todayEng.domain.diary.dto.llm;

import java.util.List;

public record DiaryMemoryAnalysisResponse(
        List<MemoryItem> people,
        List<MemoryItem> places,
        List<MemoryItem> themes,
        List<MemoryItem> ongoingStories,
        List<MemoryItem> recentEmotions
) {

    public DiaryMemoryAnalysisResponse {
        people = safe(people);
        places = safe(places);
        themes = safe(themes);
        ongoingStories = safe(ongoingStories);
        recentEmotions = safe(recentEmotions);
    }

    public record MemoryItem(
            String value,
            List<Long> sourceDiaryIds
    ) {
        public MemoryItem {
            sourceDiaryIds = safe(sourceDiaryIds);
        }
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
