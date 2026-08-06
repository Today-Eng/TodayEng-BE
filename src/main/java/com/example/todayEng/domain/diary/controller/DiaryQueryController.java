package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.response.DiaryDetailResponse;
import com.example.todayEng.domain.diary.dto.response.DiaryMonthlyListResponse;
import com.example.todayEng.domain.diary.service.DiaryQueryService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "회고 조회",
        description = "회고 목록 및 상세 조회 API"
)
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryQueryController {

    private final DiaryQueryService diaryQueryService;

    @Operation(
            summary = "월별 회고록 목록 조회",
            description = "선택한 연월의 완료된 회고록을 최신순으로 조회합니다."
    )
    @GetMapping
    public ApiResponse<DiaryMonthlyListResponse> getMonthlyDiaries(
            @AuthenticationPrincipal Long userId,

            @Parameter(
                    description = "조회 연도, 미입력 시 현재 연도",
                    example = "2026"
            )
            @RequestParam(required = false)
            Integer year,

            @Parameter(
                    description = "조회 월, 미입력 시 현재 월",
                    example = "7"
            )
            @RequestParam(required = false)
            Integer month
    ) {
        DiaryMonthlyListResponse response =
                diaryQueryService.getMonthlyDiaries(
                        userId,
                        year,
                        month
                );

        return ApiResponse.success(
                "회고록 목록을 조회했습니다.",
                response
        );
    }

    @Operation(summary = "회고 상세 조회")
    @GetMapping("/{diaryId}")
    public ApiResponse<DiaryDetailResponse> getDiaryDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long diaryId
    ) {
        DiaryDetailResponse response =
                diaryQueryService.getDiaryDetail(
                        userId,
                        diaryId
                );

        return ApiResponse.success(
                "회고 상세를 조회했습니다.",
                response
        );
    }
}
