package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.request.DiaryStartRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryStartResponse;
import com.example.todayEng.domain.diary.service.DiaryService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회고 관리", description = "회고 시작, 중단, 완료 및 메모 수정 API")
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @Operation(summary = "회고 시작")
    @PostMapping("/start")
    public ApiResponse<DiaryStartResponse> startDiary(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody DiaryStartRequest request
    ) {
        DiaryStartResponse response = diaryService.startDiary(
                userId,
                request
        );

        return ApiResponse.success(
                "회고가 시작되었습니다.",
                response
        );
    }
}
