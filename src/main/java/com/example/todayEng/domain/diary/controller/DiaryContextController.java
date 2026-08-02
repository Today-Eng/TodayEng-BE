package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryContextCreateResponse;
import com.example.todayEng.domain.diary.service.DiaryContextService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryContextController {

    private final DiaryContextService diaryContextService;

    @Operation(summary = "회고 질문 생성용 컨텍스트 생성")
    @PostMapping(value = "/{diaryId}/contexts",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DiaryContextCreateResponse> createContexts(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long diaryId,
            @Valid @RequestPart("request") DiaryContextCreateRequest request,
            @RequestPart(value = "images", required = false)
            List<MultipartFile> images
    ) {
        return ApiResponse.success(
                diaryContextService.createContexts(userId, diaryId, request, images)
        );
    }
}
