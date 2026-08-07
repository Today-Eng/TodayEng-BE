package com.example.todayEng.domain.home.controller;

import com.example.todayEng.domain.home.dto.HomeDiaryDateResponse;
import com.example.todayEng.domain.home.dto.HomeResponse;
import com.example.todayEng.domain.home.service.HomeService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "홈 화면 조회")
    @GetMapping
    public ApiResponse<HomeResponse> getHome(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ApiResponse.success(homeService.getHome(userId, year, month));
    }

    @Operation(summary = "날짜별 회고 조회")
    @GetMapping("/dates")
    public ApiResponse<HomeDiaryDateResponse> getDiaryByDate(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(homeService.getDiaryByDate(userId, date));
    }
}
