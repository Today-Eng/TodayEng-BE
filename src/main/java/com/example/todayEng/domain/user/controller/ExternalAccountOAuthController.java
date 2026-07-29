package com.example.todayEng.domain.user.controller;

import com.example.todayEng.domain.user.dto.response.OAuthAuthorizationResponse;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.service.ExternalAccountOAuthService;
import com.example.todayEng.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/external-accounts/google-calendar")
@RequiredArgsConstructor
public class ExternalAccountOAuthController {

    private final ExternalAccountOAuthService externalAccountOAuthService;

    // TODO: SecurityUtil이 구현되면 userId 파라미터를 제거하고 인증 정보에서 추출하도록 변경
    @PostMapping("/authorization")
    public ApiResponse<OAuthAuthorizationResponse> createAuthorizationUrl(
            @RequestParam Long userId
    ) {
        return ApiResponse.success(
                externalAccountOAuthService.createAuthorizationUrl(
                        userId,
                        ExternalServiceProvider.GOOGLE_CALENDAR
                )
        );
    }

    @GetMapping("/callback")
    public ApiResponse<Void> handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam String state,
            @RequestParam(required = false) String error
    ) {
        externalAccountOAuthService.connectExternalAccount(
                ExternalServiceProvider.GOOGLE_CALENDAR,
                code,
                state,
                error
        );

        return ApiResponse.success();
    }
}
