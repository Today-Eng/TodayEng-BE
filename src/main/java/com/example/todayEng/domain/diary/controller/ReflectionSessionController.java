package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse;
import com.example.todayEng.domain.diary.service.ReflectionSessionService;
import com.example.todayEng.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diaries")
@RequiredArgsConstructor
public class ReflectionSessionController {

    private final ReflectionSessionService reflectionSessionService;

    @PostMapping("/{diaryId}/reflection-sessions")
    public ApiResponse<ReflectionSessionResponse> create(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long diaryId
    ) {
        return ApiResponse.success(
                "맞춤 회고 질문이 생성되었습니다.",
                reflectionSessionService.create(userId, diaryId)
        );
    }
}
