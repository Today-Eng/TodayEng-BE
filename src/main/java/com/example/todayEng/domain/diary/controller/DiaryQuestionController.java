package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.response.DiaryQuestionListResponse;
import com.example.todayEng.domain.diary.dto.response.NextDiaryQuestionResponse;
import com.example.todayEng.domain.diary.service.DiaryQuestionQueryService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diaries")
@RequiredArgsConstructor
@Tag(name = "회고 진행", description = "질문 생성·조회, 음성 답변 및 진행 결과 구독 API")
public class DiaryQuestionController {
    private final DiaryQuestionQueryService queryService;

    @Operation(summary = "질문 목록과 진행 상태 조회", description = "질문을 1~6 순서로 반환합니다. Answer가 아직 없으면 답변 관련 필드는 생략됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "A004: Diary 소유자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "D011: Diary 없음")
    })
    @GetMapping("/{diaryId}/questions")
    public ApiResponse<DiaryQuestionListResponse> getQuestions(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "회고 ID", example = "1") @PathVariable Long diaryId) {
        return ApiResponse.success("회고 질문 목록을 조회했습니다.", queryService.getQuestions(userId, diaryId));
    }

    @Operation(summary = "현재 질문 조회", description = "순서상 현재 답변 가능한 질문을 반환합니다. status는 WAITING, QUESTION_READY, READY_TO_COMPLETE 중 하나입니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "A004: Diary 소유자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "D011: Diary 없음")
    })
    @GetMapping("/{diaryId}/questions/next")
    public ApiResponse<NextDiaryQuestionResponse> getNextQuestion(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "회고 ID", example = "1") @PathVariable Long diaryId) {
        return ApiResponse.success("현재 회고 질문을 조회했습니다.", queryService.getNextQuestion(userId, diaryId));
    }
}
