package com.example.todayEng.domain.user.controller;

import com.example.todayEng.domain.user.dto.response.ExternalAccountListResponse;
import com.example.todayEng.domain.user.service.ExternalAccountService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "외부 서비스 연동 관리", description = "외부 서비스 연동 상태 조회, 사용 설정 변경 및 연동 해제 API")
@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class ExternalAccountStatusController {

    private final ExternalAccountService externalAccountService;

    @Operation(
            summary = "연동 서비스 상태 조회",
            description = "지원하는 모든 외부 서비스의 연동 상태를 반환합니다. 연동되지 않은 서비스는 connected=false 로 내려갑니다."
    )
    @GetMapping("/external-accounts")
    public ApiResponse<ExternalAccountListResponse> getExternalAccounts(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                externalAccountService.getExternalAccounts(userId)
        );
    }
}
