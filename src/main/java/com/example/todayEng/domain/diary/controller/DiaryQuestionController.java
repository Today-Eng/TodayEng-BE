package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.response.DiaryQuestionListResponse;
import com.example.todayEng.domain.diary.dto.response.NextDiaryQuestionResponse;
import com.example.todayEng.domain.diary.service.DiaryQuestionQueryService;
import com.example.todayEng.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diaries")
@RequiredArgsConstructor
public class DiaryQuestionController {
    private final DiaryQuestionQueryService queryService;

    @GetMapping("/{diaryId}/questions")
    public ApiResponse<DiaryQuestionListResponse> getQuestions(
            @AuthenticationPrincipal Long userId, @PathVariable Long diaryId) {
        return ApiResponse.success("회고 질문 목록을 조회했습니다.", queryService.getQuestions(userId, diaryId));
    }

    @GetMapping("/{diaryId}/questions/next")
    public ApiResponse<NextDiaryQuestionResponse> getNextQuestion(
            @AuthenticationPrincipal Long userId, @PathVariable Long diaryId) {
        return ApiResponse.success("현재 회고 질문을 조회했습니다.", queryService.getNextQuestion(userId, diaryId));
    }
}
