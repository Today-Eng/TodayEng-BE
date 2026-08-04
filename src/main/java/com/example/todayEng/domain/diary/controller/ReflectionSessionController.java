package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse;
import com.example.todayEng.domain.diary.service.ReflectionSessionService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diaries")
@RequiredArgsConstructor
@Tag(name = "회고 진행", description = "질문 생성·조회, 음성 답변 및 진행 결과 구독 API")
public class ReflectionSessionController {

    private final ReflectionSessionService reflectionSessionService;

    @Operation(summary = "기본 질문 3개 생성", description = "DiaryContext와 사용자 영어 수준을 이용해 MAIN 질문 3개를 생성합니다. 중복 호출할 수 없습니다. 생성 후 첫 질문 TTS가 실행됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "질문 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "D014: 사용할 컨텍스트 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "A004: Diary 소유자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "D012/D013: 진행 불가 또는 중복 생성"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "D015~D017: LLM 호출 또는 응답 오류")
    })
    @PostMapping("/{diaryId}/reflection-sessions")
    public ApiResponse<ReflectionSessionResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "회고 ID", example = "1") @PathVariable Long diaryId
    ) {
        return ApiResponse.success(
                "맞춤 회고 질문이 생성되었습니다.",
                reflectionSessionService.create(userId, diaryId)
        );
    }
}
