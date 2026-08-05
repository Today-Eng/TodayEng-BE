package com.example.todayEng.domain.diary.dto.sse;

import java.time.LocalDateTime;

public record DiarySseEvent<T>(
        String type,
        Long diaryId,
        LocalDateTime occurredAt,
        T data
) {

    public static <T> DiarySseEvent<T> of(
            String type,
            Long diaryId,
            T data
    ) {
        return new DiarySseEvent<>(
                type,
                diaryId,
                LocalDateTime.now(),
                data
        );
    }
}
