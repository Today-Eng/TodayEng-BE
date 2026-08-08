package com.example.todayEng.domain.user.controller;

import com.example.todayEng.domain.user.dto.request.ExternalAccountUseEnabledUpdateRequest;
import com.example.todayEng.domain.user.dto.response.ExternalAccountSettingsResponse;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.service.ExternalAccountService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "외부 서비스 연동 관리", description = "외부 서비스 연동 상태 조회, 사용 설정 변경 및 연동 해제 API")
@RestController
@RequestMapping("/api/external-accounts")
@RequiredArgsConstructor
public class ExternalAccountController {

    private final ExternalAccountService externalAccountService;

    @Operation(
            summary = "외부 서비스 사용 설정 변경",
            description = "계정 연동은 유지한 채 해당 외부 서비스 데이터를 회고 질문 생성에 사용할지를 변경합니다."
    )
    @PatchMapping("/{provider}/settings")
    public ApiResponse<ExternalAccountSettingsResponse> updateUseEnabled(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,

            @Parameter(description = "외부 서비스 Provider", example = "SPOTIFY")
            @PathVariable String provider,

            @Valid @RequestBody ExternalAccountUseEnabledUpdateRequest request
    ) {
        return ApiResponse.success(
                "외부 서비스 연동 설정이 변경되었습니다.",
                externalAccountService.updateUseEnabled(
                        userId,
                        ExternalServiceProvider.from(provider),
                        request.useEnabled()
                )
        );
    }

    @Operation(
            summary = "외부 서비스 연동 해제",
            description = "이미 해제된 상태에서 다시 호출해도 200을 반환하는 멱등 API입니다."
    )
    @DeleteMapping("/{provider}")
    public ApiResponse<Void> disconnect(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,

            @Parameter(description = "외부 서비스 Provider", example = "SPOTIFY")
            @PathVariable String provider
    ) {
        externalAccountService.disconnect(
                userId,
                ExternalServiceProvider.from(provider)
        );

        return ApiResponse.success(
                "외부 서비스 연동이 해제되었습니다.",
                null
        );
    }
}
