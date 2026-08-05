package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.response.AnswerUploadResponse;
import com.example.todayEng.domain.diary.dto.response.DiaryAnswerDetailResponse;
import com.example.todayEng.domain.diary.dto.response.DiaryAnswerListResponse;
import com.example.todayEng.domain.diary.service.DiaryAnswerQueryService;
import com.example.todayEng.domain.diary.service.AnswerUploadService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "회고 진행", description = "질문 생성·조회, 음성 답변 및 진행 결과 구독 API")
public class DiaryAnswerController {
    private final AnswerUploadService answerUploadService;
    private final DiaryAnswerQueryService answerQueryService;

    @Operation(summary = "답변 처리 결과 단건 조회", description = "STT 원문, 영어 교정 결과와 현재 처리 상태를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "A004: Diary 소유자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "D011/D025: Diary 또는 Answer 없음")
    })
    @GetMapping("/{diaryId}/answers/{answerId}")
    public ApiResponse<DiaryAnswerDetailResponse> getAnswer(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "회고 ID", example = "1") @PathVariable Long diaryId,
            @Parameter(description = "답변 ID", example = "20") @PathVariable Long answerId) {
        return ApiResponse.success("회고 답변 처리 결과를 조회했습니다.",
                answerQueryService.getAnswer(userId, diaryId, answerId));
    }

    @Operation(summary = "전체 회고 답변 조회", description = "Diary의 답변을 질문 순서대로 반환합니다. expectedAnswerCount는 현재 생성된 질문 수입니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "A004: Diary 소유자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "D011: Diary 없음")
    })
    @GetMapping("/{diaryId}/answers")
    public ApiResponse<DiaryAnswerListResponse> getAnswers(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "회고 ID", example = "1") @PathVariable Long diaryId) {
        return ApiResponse.success("전체 회고 답변을 조회했습니다.",
                answerQueryService.getAnswers(userId, diaryId));
    }

    @Operation(summary = "WebM 음성 답변 업로드", description = "audio/webm(WebM/Opus) 파일만 허용하며 최대 10MB입니다. 임시 저장 후 202를 반환하고 STT·교정을 비동기로 실행합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "업로드 완료, 비동기 처리 시작"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C009/E002/D024: 파일 누락, 용량 초과 또는 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "D018: Question 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "D022/D023: 답변 불가 또는 중복 답변")
    })
    @PostMapping(value = "/{diaryId}/questions/{questionId}/answers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AnswerUploadResponse>> upload(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "회고 ID", example = "1") @PathVariable Long diaryId,
            @Parameter(description = "현재 질문 ID", example = "10") @PathVariable Long questionId,
            @Parameter(description = "WebM/Opus 음성 파일 (Content-Type: audio/webm, 최대 10MB)")
            @RequestPart("audio") MultipartFile audio) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(
                "음성 답변 처리를 시작했습니다.", answerUploadService.upload(userId, diaryId, questionId, audio)));
    }
}
