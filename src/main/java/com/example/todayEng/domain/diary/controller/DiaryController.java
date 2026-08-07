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

@Tag(name = "회고 관리", description = "회고 시작·재개, 완료, 메모 수정 및 삭제 API")
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private static final String DIARY_STARTED_MESSAGE = "회고가 시작되었습니다.";
    private static final String DIARY_RESUMED_MESSAGE = "작성 중인 회고를 불러왔습니다.";

    private final DiaryService diaryService;

    @Operation(summary = "회고 시작", description = "해당 날짜의 회고를 시작합니다. 작성 중인 회고가 이미 있으면 새로 만들지 않고 그대로 재개하며 resumed=true를 반환합니다. 오늘 포함 7일 이내 날짜만 요청할 수 있고, 이미 완료된 날짜는 재작성할 수 없습니다.")
    @PostMapping
    public ApiResponse<DiaryStartResponse> startDiary(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody DiaryStartRequest request
    ) {
        DiaryStartResponse response = diaryService.startDiary(
                userId,
                request
        );

        return ApiResponse.success(
                response.resumed() ? DIARY_RESUMED_MESSAGE : DIARY_STARTED_MESSAGE,
                response
        );
    }
}
