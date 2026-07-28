package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.request.DiaryStartRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryStartResponse;
import com.example.todayEng.domain.diary.service.DiaryService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Diary", description = "회고 관리 API")
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    // TODO: SecurityUtil 구현 후 userId 요청 파라미터를 제거하고 인증 사용자 ID를 사용
    @Operation(summary = "회고 시작")
    @PostMapping("/start")
    public ApiResponse<DiaryStartResponse> startDiary(
            @RequestParam Long userId,
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
