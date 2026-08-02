package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.request.DiaryCompleteRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryCompleteResponse;
import com.example.todayEng.domain.diary.service.DiaryCompletionService;
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
@Tag(name = "회고 완료", description = "모든 질문과 교정 완료 후 최종 메모 저장")
public class DiaryCompletionController {
    private final DiaryCompletionService completionService;

    @Operation(summary = "일기 최종 저장", description = "MAIN 3개, FOLLOW_UP 3개와 교정 완료 답변 6개를 검증한 뒤 finalMemo를 Diary.memo에 저장합니다. 재호출은 기존 완료 결과를 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "완료 성공 또는 기존 완료 결과 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: finalMemo가 2000자 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "A004: Diary 소유자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "D011: Diary 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "D030~D032: 질문 구성, 답변 또는 교정 미완료")
    })
    @PatchMapping("/{diaryId}/complete")
    public ApiResponse<DiaryCompleteResponse> complete(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "회고 ID", example = "1") @PathVariable Long diaryId,
            @Valid @RequestBody DiaryCompleteRequest request) {
        return ApiResponse.success("일기를 최종 저장했습니다.", completionService.complete(userId, diaryId, request));
    }
}
