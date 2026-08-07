package com.example.todayEng.domain.diary.dto.request;

public record DiaryContextCreateRequest(
        String memo,
        Location location
) {
    public record Location(double latitude, double longitude) {
    }
}
