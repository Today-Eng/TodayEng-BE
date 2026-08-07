package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.service.DiaryDeletionService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
@Tag(name = "회고 관리", description = "회고 시작, 중단, 완료 및 삭제 API")
public class DiaryDeletionController {

    private final DiaryDeletionService diaryDeletionService;

    @Operation(summary = "회고 삭제", description = "완료된 회고를 삭제합니다. 회고 내용(질문/답변/컨텍스트)은 모두 삭제되며 같은 날짜에 재작성은 불가합니다.")
    @DeleteMapping("/{diaryId}")
    public ApiResponse<Void> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "회고 ID", example = "1") @PathVariable Long diaryId
    ) {
        diaryDeletionService.delete(userId, diaryId);
        return ApiResponse.success();
    }
}
