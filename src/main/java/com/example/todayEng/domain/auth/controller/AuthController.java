package com.example.todayEng.domain.auth.controller;

import com.example.todayEng.domain.auth.dto.*;
import com.example.todayEng.domain.auth.service.AuthService;
import com.example.todayEng.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/google")
    public ApiResponse<LoginResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
        return ApiResponse.success(authService.googleLogin(request.idToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Long userId,
                                    @Valid @RequestBody LogoutRequest request) {
        authService.logout(userId, request.refreshToken());
        return ApiResponse.success();
    }
}
