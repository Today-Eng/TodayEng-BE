package com.example.todayEng.domain.diary.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record DiaryImageAnalysis(List<PhotoContext> photoContexts) {

    public DiaryImageAnalysis {
        photoContexts = photoContexts == null ? List.of() : List.copyOf(photoContexts);
    }

    public record PhotoContext(List<Integer> sourceImageIndexes, JsonNode contextData) {

        public PhotoContext {
            sourceImageIndexes = sourceImageIndexes == null
                    ? List.of()
                    : List.copyOf(sourceImageIndexes);
        }
    }
}
