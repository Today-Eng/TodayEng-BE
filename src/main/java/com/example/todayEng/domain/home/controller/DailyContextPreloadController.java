package com.example.todayEng.domain.home.controller;

import com.example.todayEng.domain.home.dto.DailyContextPreloadRequest;
import com.example.todayEng.domain.home.service.DailyContextPreloadService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/daily-contexts")
@RequiredArgsConstructor
public class DailyContextPreloadController {

    private final DailyContextPreloadService preloadService;

    @Operation(summary = "오늘의 컨텍스트 사전 수집")
    @PostMapping("/preload")
    public ApiResponse<Void> preload(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody(required = false) DailyContextPreloadRequest request
    ) {
        preloadService.preload(userId, request == null ? null : request.location());
        return ApiResponse.success();
    }
}
