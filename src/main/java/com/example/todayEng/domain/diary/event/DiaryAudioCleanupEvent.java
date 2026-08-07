package com.example.todayEng.domain.diary.event;

import java.util.ArrayList;
import java.util.List;

public record DiaryAudioCleanupEvent(
        List<String> audioKeys
) {

    public static DiaryAudioCleanupEvent of(
            List<String> answerAudioKeys,
            List<String> ttsAudioKeys
    ) {
        List<String> merged = new ArrayList<>(
                answerAudioKeys.size() + ttsAudioKeys.size()
        );

        merged.addAll(answerAudioKeys);
        merged.addAll(ttsAudioKeys);

        return new DiaryAudioCleanupEvent(
                List.copyOf(merged)
        );
    }
}
