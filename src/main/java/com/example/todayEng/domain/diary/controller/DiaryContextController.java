package com.example.todayEng.domain.diary.controller;

import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryContextCreateResponse;
import com.example.todayEng.domain.diary.service.DiaryContextService;
import com.example.todayEng.global.common.ApiResponse;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "회고 컨텍스트", description = "회고 질문 생성에 사용할 컨텍스트 수집 API")
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryContextController {

    private final DiaryContextService diaryContextService;
    private final ObjectMapper objectMapper;

    @Operation(
            summary = "회고 질문 생성용 컨텍스트 생성",
            description = "multipart/form-data로 요청합니다. "
                    + "request part는 memo와 location을 담은 JSON 문자열이며 생략할 수 있습니다. "
                    + "images part는 jpeg·png·webp만 허용하고 최대 2장, 장당 7MB, 합계 14MB입니다. "
                    + "서로 다른 사진은 개별 PHOTO 컨텍스트로 저장하며, 실질적으로 같은 장면은 하나로 병합할 수 있습니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schemaProperties = {
                            @SchemaProperty(
                                    name = "request",
                                    schema = @Schema(implementation = DiaryContextCreateRequest.class)
                            ),
                            @SchemaProperty(
                                    name = "images",
                                    array = @ArraySchema(
                                            maxItems = 2,
                                            schema = @Schema(type = "string", format = "binary")
                                    )
                            )
                    },
                    encoding = @Encoding(
                            name = "request",
                            contentType = MediaType.APPLICATION_JSON_VALUE
                    )
            )
    )
    @PostMapping(value = "/{diaryId}/contexts",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DiaryContextCreateResponse> createContexts(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "회고 ID", example = "1") @PathVariable Long diaryId,
            @Parameter(hidden = true)
            @RequestPart(value = "request", required = false) String request,
            @Parameter(hidden = true)
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ApiResponse.success(
                diaryContextService.createContexts(
                        userId, diaryId, parseRequest(request), images)
        );
    }

    // part별 Content-Type 없이 오는 요청도 받기 위해 객체 바인딩을 쓰지 않는다
    private DiaryContextCreateRequest parseRequest(String request) {
        if (request == null || request.isBlank()) {
            return emptyRequest();
        }
        try {
            DiaryContextCreateRequest parsed =
                    objectMapper.readValue(request, DiaryContextCreateRequest.class);
            return parsed == null ? emptyRequest() : parsed;
        } catch (JsonProcessingException exception) {
            throw new BaseException(ErrorCode.INVALID_HTTP_BODY);
        }
    }

    private DiaryContextCreateRequest emptyRequest() {
        return new DiaryContextCreateRequest(null, null);
    }
}
