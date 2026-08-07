package com.example.todayEng.domain.auth.controller;

import com.example.todayEng.domain.auth.dto.*;
import com.example.todayEng.domain.auth.service.AuthService;
import com.example.todayEng.global.common.ApiResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "Google 로그인 및 로그아웃 API")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Google 로그인", description = "프론트엔드가 Google에서 발급받은 ID Token을 검증하고 서비스 JWT를 발급합니다.")
    @SecurityRequirements
    @PostMapping("/google")
    public ApiResponse<LoginResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
        return ApiResponse.success(authService.googleLogin(request.idToken()));
    }

    @Operation(summary = "토큰 재발급", description = "유효한 Refresh Token을 검증하고 Access Token과 Refresh Token을 새로 발급합니다.")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "로그아웃", description = "현재 사용자에게 발급된 Refresh Token을 폐기합니다. Access Token 인증이 필요합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Long userId,
                                    @Valid @RequestBody LogoutRequest request) {
        authService.logout(userId, request.refreshToken());
        return ApiResponse.success();
    }
}
