package com.example.todayEng.domain.auth.controller;

import com.example.todayEng.domain.auth.dto.LoginResponse;
import com.example.todayEng.domain.auth.dto.TestLoginRequest;
import com.example.todayEng.domain.auth.service.AuthService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "회원 테스트 로그인", description = "로컬 환경에서 Google 인증 없이 회원 API를 테스트하기 위한 로그인")
public class TestAuthController {
    private final AuthService authService;

    @Operation(
            summary = "테스트 로그인",
            description = "Try it out을 누른 뒤 바로 실행할 수 있습니다. 응답의 accessToken을 복사하여 우측 상단 Authorize에 입력하면 회원 API를 테스트할 수 있습니다. 같은 socialUid를 사용하면 같은 회원으로 로그인합니다."
    )
    @SecurityRequirements
    @PostMapping("/test")
    public ApiResponse<LoginResponse> testLogin(@Valid @RequestBody TestLoginRequest request) {
        return ApiResponse.success(authService.testLogin(request.socialUid()));
    }
}
