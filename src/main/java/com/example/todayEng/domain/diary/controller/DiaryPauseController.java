package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.response.DiaryPauseResponse;
import com.example.todayEng.domain.diary.service.DiaryPauseService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diaries")
@RequiredArgsConstructor
@Tag(name = "회고 관리", description = "회고 시작, 중단 및 완료 API")
public class DiaryPauseController {

    private final DiaryPauseService diaryPauseService;

    @Operation(summary = "회고 중단", description = "현재 진행 상태를 보존하고 회고를 중단합니다. 24시간 동안 재개할 수 있습니다.")
    @PatchMapping("/{diaryId}/pause")
    public ApiResponse<DiaryPauseResponse> pause(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "회고 ID", example = "1") @PathVariable Long diaryId
    ) {
        return ApiResponse.success(
                "회고가 중단되었습니다.",
                diaryPauseService.pause(userId, diaryId)
        );
    }
}
