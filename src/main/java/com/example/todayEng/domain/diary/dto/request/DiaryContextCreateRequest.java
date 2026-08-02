package com.example.todayEng.domain.diary.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record DiaryContextCreateRequest(
        @Size(max = 200, message = "메모는 최대 200자까지 입력할 수 있습니다.")
        String memo,

        @Valid Location location
) {
    public record Location(double latitude, double longitude) {
    }
}
