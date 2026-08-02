package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.service.DiarySubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Diary SSE", description = "회고 진행 결과 구독 API")
@RestController
@RequestMapping("/diaries")
@RequiredArgsConstructor
public class DiarySseController {

    private final DiarySubscriptionService subscriptionService;

    @Operation(summary = "회고 진행 결과 구독")
    @GetMapping(
            value = "/{diaryId}/subscribe",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter subscribe(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long diaryId
    ) {
        return subscriptionService.subscribe(userId, diaryId);
    }
}
