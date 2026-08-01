package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.request.DiaryStartRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryDetailResponse;
import com.example.todayEng.domain.diary.dto.response.DiaryMonthlyListResponse;
import com.example.todayEng.domain.diary.dto.response.DiaryStartResponse;
import com.example.todayEng.domain.diary.service.DiaryQueryService;
import com.example.todayEng.domain.diary.service.DiaryService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Diary", description = "회고 관리 API")
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;
    private final DiaryQueryService diaryQueryService;

    // TODO: SecurityUtil 구현 후 userId 요청 파라미터를 제거하고 인증 사용자 ID를 사용
    @Operation(summary = "회고 시작")
    @PostMapping("/start")
    public ApiResponse<DiaryStartResponse> startDiary(
            @RequestParam Long userId,
            @Valid @RequestBody DiaryStartRequest request
    ) {
        DiaryStartResponse response = diaryService.startDiary(
                userId,
                request
        );

        return ApiResponse.success(
                "회고가 시작되었습니다.",
                response
        );
    }

    // TODO: SecurityUtil 구현 후 userId 요청 파라미터를 제거하고 인증 사용자 ID를 사용
    @Operation(
            summary = "월별 회고록 목록 조회",
            description = "선택한 연월의 완료된 회고록을 최신순으로 조회합니다."
    )
    @GetMapping
    public ApiResponse<DiaryMonthlyListResponse> getMonthlyDiaries(
            @RequestParam Long userId,

            @Parameter(description = "조회 연도, 미입력 시 현재 연도", example = "2026")
            @RequestParam(required = false) Integer year,

            @Parameter(description = "조회 월, 미입력 시 현재 월", example = "7")
            @RequestParam(required = false) Integer month
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
            @RequestParam Long userId,
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
