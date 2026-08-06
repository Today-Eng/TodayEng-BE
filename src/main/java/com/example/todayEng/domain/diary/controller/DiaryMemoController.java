package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.request.DiaryMemoUpdateRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryMemoUpdateResponse;
import com.example.todayEng.domain.diary.service.DiaryMemoService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diaries")
@RequiredArgsConstructor
@Tag(name = "회고 관리", description = "회고 시작, 중단, 완료 및 메모 수정 API")
public class DiaryMemoController {

    private final DiaryMemoService diaryMemoService;

    @Operation(summary = "회고 메모 수정", description = "완료된 회고의 메모를 수정합니다. null 또는 공백만 입력하면 메모를 삭제합니다.")
    @PatchMapping("/{diaryId}/memo")
    public ApiResponse<DiaryMemoUpdateResponse> updateMemo(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "회고 ID", example = "1") @PathVariable Long diaryId,
            @Valid @RequestBody DiaryMemoUpdateRequest request
    ) {
        return ApiResponse.success(
                "회고 메모를 수정했습니다.",
                diaryMemoService.updateMemo(userId, diaryId, request)
        );
    }
}
