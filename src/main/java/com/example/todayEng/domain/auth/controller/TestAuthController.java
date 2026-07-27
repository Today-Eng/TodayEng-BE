package com.example.todayEng.domain.auth.controller;

import com.example.todayEng.domain.auth.dto.LoginResponse;
import com.example.todayEng.domain.auth.dto.TestLoginRequest;
import com.example.todayEng.domain.auth.service.AuthService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class TestAuthController {
    private final AuthService authService;

    @Operation(
            summary = "[LOCAL] 테스트 로그인",
            description = "Google 인증 없이 socialUid로 테스트 사용자를 생성하거나 조회하고 JWT를 발급합니다. local 프로필에서만 노출됩니다."
    )
    @SecurityRequirements
    @PostMapping("/test")
    public ApiResponse<LoginResponse> testLogin(@Valid @RequestBody TestLoginRequest request) {
        return ApiResponse.success(authService.testLogin(request.socialUid()));
    }
}
