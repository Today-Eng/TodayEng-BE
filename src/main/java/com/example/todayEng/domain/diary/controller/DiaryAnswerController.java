package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.response.AnswerUploadResponse;
import com.example.todayEng.domain.diary.dto.response.DiaryAnswerDetailResponse;
import com.example.todayEng.domain.diary.dto.response.DiaryAnswerListResponse;
import com.example.todayEng.domain.diary.service.DiaryAnswerQueryService;
import com.example.todayEng.domain.diary.service.AnswerUploadService;
import com.example.todayEng.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/diaries")
@RequiredArgsConstructor
public class DiaryAnswerController {
    private final AnswerUploadService answerUploadService;
    private final DiaryAnswerQueryService answerQueryService;

    @GetMapping("/{diaryId}/answers/{answerId}")
    public ApiResponse<DiaryAnswerDetailResponse> getAnswer(
            @AuthenticationPrincipal Long userId, @PathVariable Long diaryId, @PathVariable Long answerId) {
        return ApiResponse.success("회고 답변 처리 결과를 조회했습니다.",
                answerQueryService.getAnswer(userId, diaryId, answerId));
    }

    @GetMapping("/{diaryId}/answers")
    public ApiResponse<DiaryAnswerListResponse> getAnswers(
            @AuthenticationPrincipal Long userId, @PathVariable Long diaryId) {
        return ApiResponse.success("전체 회고 답변을 조회했습니다.",
                answerQueryService.getAnswers(userId, diaryId));
    }

    @PostMapping(value = "/{diaryId}/questions/{questionId}/answers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AnswerUploadResponse>> upload(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long diaryId,
            @PathVariable Long questionId,
            @RequestPart("audio") MultipartFile audio) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(
                "음성 답변 처리를 시작했습니다.", answerUploadService.upload(userId, diaryId, questionId, audio)));
    }
}
