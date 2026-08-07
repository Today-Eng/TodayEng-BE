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
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
@Tag(name = "회고 관리", description = "회고 시작, 중단, 완료 및 메모 수정 API")
public class DiaryPauseController {

    private final DiaryPauseService diaryPauseService;

    @Operation(summary = "회고 중간 종료", description = "현재까지 저장된 내용을 보존하고 답변 개수와 무관하게 회고를 완료합니다. 완료 후에는 재개할 수 없습니다.")
    @PatchMapping("/{diaryId}/pause")
    public ApiResponse<DiaryPauseResponse> pause(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "회고 ID", example = "1") @PathVariable Long diaryId
    ) {
        return ApiResponse.success(
                "회고가 중간 종료되었습니다.",
                diaryPauseService.pause(userId, diaryId)
        );
    }
}
