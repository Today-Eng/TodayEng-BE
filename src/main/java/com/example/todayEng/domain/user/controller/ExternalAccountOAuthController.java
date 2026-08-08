package com.example.todayEng.domain.user.controller;

import com.example.todayEng.domain.user.dto.response.OAuthAuthorizationResponse;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.service.ExternalAccountOAuthService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "외부 서비스 OAuth", description = "외부 서비스 OAuth 연동 URL 발급 API")
@RestController
@RequestMapping("/api/external-accounts/{provider}")
@RequiredArgsConstructor
public class ExternalAccountOAuthController {

    private final ExternalAccountOAuthService externalAccountOAuthService;

    @Operation(
            summary = "외부 서비스 연동 URL 발급",
            description = "OAuth state를 발급하고 외부 서비스 인가 URL을 반환합니다. 프론트는 응답받은 authorizationUrl을 새 창 또는 웹뷰로 열면 됩니다."
    )
    @PostMapping("/authorization")
    public ApiResponse<OAuthAuthorizationResponse> createAuthorizationUrl(
            @Parameter(description = "외부 서비스 Provider", example = "google-calendar")
            @PathVariable String provider,

            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                externalAccountOAuthService.createAuthorizationUrl(
                        userId,
                        ExternalServiceProvider.from(provider)
                )
        );
    }

    @Hidden
    @GetMapping("/callback")
    public ApiResponse<Void> handleCallback(
            @Parameter(description = "외부 서비스 Provider", example = "google-calendar")
            @PathVariable String provider,

            @Parameter(description = "외부 서비스가 발급한 Authorization Code (사용자가 동의한 경우)")
            @RequestParam(required = false) String code,

            @Parameter(description = "인가 URL 발급 시 서버가 생성한 OAuth state 원문")
            @RequestParam String state,

            @Parameter(description = "사용자가 동의를 거부한 경우 외부 서비스가 전달하는 에러 코드 (예: access_denied)")
            @RequestParam(required = false) String error
    ) {
        externalAccountOAuthService.connectExternalAccount(
                ExternalServiceProvider.from(provider),
                code,
                state,
                error
        );

        return ApiResponse.success();
    }
}
