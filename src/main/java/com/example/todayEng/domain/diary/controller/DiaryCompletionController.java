package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.request.DiaryCompleteRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryCompleteResponse;
import com.example.todayEng.domain.diary.service.DiaryCompletionService;
import com.example.todayEng.global.common.ApiResponse;
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
public class DiaryCompletionController {
    private final DiaryCompletionService completionService;

    @PatchMapping("/{diaryId}/complete")
    public ApiResponse<DiaryCompleteResponse> complete(@AuthenticationPrincipal Long userId,
            @PathVariable Long diaryId, @Valid @RequestBody DiaryCompleteRequest request) {
        return ApiResponse.success("일기를 최종 저장했습니다.", completionService.complete(userId, diaryId, request));
    }
}
