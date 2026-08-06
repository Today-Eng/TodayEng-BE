package com.example.todayEng.domain.home.dto;

import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest.Location;
import jakarta.validation.Valid;

public record DailyContextPreloadRequest(@Valid Location location) {
}
