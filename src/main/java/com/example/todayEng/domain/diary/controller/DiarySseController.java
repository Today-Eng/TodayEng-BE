package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.service.DiarySubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "회고 진행", description = "질문 생성·조회, 음성 답변 및 진행 결과 구독 API")
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiarySseController {

    private final DiarySubscriptionService subscriptionService;

    @Operation(summary = "회고 진행 결과 SSE 구독", description = "연결 직후 connected 이벤트를 전송합니다. Swagger UI는 장시간 스트림 표시가 제한될 수 있어 실제 이벤트 확인은 curl 또는 프론트 EventSource 사용을 권장합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "text/event-stream 연결"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "A004: Diary 소유자가 아님")
    })
    @GetMapping(
            value = "/{diaryId}/subscribe",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter subscribe(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "구독할 회고 ID", example = "1") @PathVariable Long diaryId
    ) {
        return subscriptionService.subscribe(userId, diaryId);
    }
}
