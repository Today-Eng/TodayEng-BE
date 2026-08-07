package com.example.todayEng.domain.home.controller;

import com.example.todayEng.domain.home.dto.DailyContextPreloadRequest;
import com.example.todayEng.domain.home.dto.DailyContextPreloadResponse;
import com.example.todayEng.domain.home.service.DailyContextPreloadService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회고 컨텍스트", description = "회고 질문 생성에 사용할 컨텍스트 수집 API")
@RestController
@RequestMapping("/api/daily-contexts")
@RequiredArgsConstructor
public class DailyContextPreloadController {

    private final DailyContextPreloadService preloadService;

    @Operation(summary = "오늘의 컨텍스트 사전 수집",
            description = "오늘 날짜의 날씨·캘린더·Spotify 컨텍스트를 미리 수집합니다. "
                    + "오늘 회고가 이미 시작됐으면 수집하지 않고 skipReason을 반환하며, "
                    + "개별 수집 실패는 해당 타입의 status로만 표시되고 응답은 200을 유지합니다.")
    @PostMapping("/preload")
    public ApiResponse<DailyContextPreloadResponse> preload(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody(required = false) DailyContextPreloadRequest request
    ) {
        return ApiResponse.success(
                preloadService.preload(userId, request == null ? null : request.location())
        );
    }
}
