package com.example.todayEng.domain.user.controller;

import com.example.todayEng.domain.user.dto.request.ExternalAccountUseEnabledUpdateRequest;
import com.example.todayEng.domain.user.dto.response.ExternalAccountResponse;
import com.example.todayEng.domain.user.service.ExternalAccountService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "External Account", description = "외부 서비스 연동 관리 API")
@RestController
@RequestMapping("/api/external-accounts")
@RequiredArgsConstructor
public class ExternalAccountController {

    private final ExternalAccountService externalAccountService;

    // TODO: SecurityUtil이 구현되면 userId 파라미터를 제거하고 인증 정보에서 추출하도록 변경
    @Operation(summary = "외부 계정 연동 상태 조회")
    @GetMapping
    public ApiResponse<List<ExternalAccountResponse>> getExternalAccounts(
            @RequestParam Long userId
    ) {
        return ApiResponse.success(externalAccountService.getExternalAccounts(userId));
    }

    // TODO: SecurityUtil이 구현되면 userId 파라미터를 제거하고 인증 정보에서 추출하도록 변경
    @Operation(summary = "외부 서비스 사용 설정 변경")
    @PatchMapping("/{externalAccountId}/use-enabled")
    public ApiResponse<Void> updateUseEnabled(
            @RequestParam Long userId,
            @PathVariable Long externalAccountId,
            @Valid @RequestBody ExternalAccountUseEnabledUpdateRequest request
    ) {
        externalAccountService.updateUseEnabled(userId, externalAccountId, request.useEnabled());
        return ApiResponse.success();
    }

    // TODO: SecurityUtil이 구현되면 userId 파라미터를 제거하고 인증 정보에서 추출하도록 변경
    @Operation(summary = "외부 서비스 연동 해제")
    @DeleteMapping("/{externalAccountId}")
    public ApiResponse<Void> disconnect(
            @RequestParam Long userId,
            @PathVariable Long externalAccountId
    ) {
        externalAccountService.disconnect(userId, externalAccountId);
        return ApiResponse.success();
    }
}
