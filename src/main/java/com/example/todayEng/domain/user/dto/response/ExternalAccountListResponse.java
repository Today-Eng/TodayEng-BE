package com.example.todayEng.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ExternalAccountListResponse(
        @Schema(description = "지원하는 모든 외부 서비스의 연동 상태. 연동되지 않은 서비스도 포함됩니다.")
        List<ExternalAccountResponse> externalAccounts
) {
}
