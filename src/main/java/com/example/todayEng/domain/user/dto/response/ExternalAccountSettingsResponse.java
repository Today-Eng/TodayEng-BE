package com.example.todayEng.domain.user.dto.response;

import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import io.swagger.v3.oas.annotations.media.Schema;

public record ExternalAccountSettingsResponse(
        @Schema(description = "외부 서비스 Provider", example = "SPOTIFY")
        ExternalServiceProvider provider,

        @Schema(description = "계정 연동 여부", example = "true")
        boolean connected,

        @Schema(description = "회고 질문 생성에 이 서비스 데이터를 사용할지 여부", example = "false")
        boolean useEnabled
) {

    public static ExternalAccountSettingsResponse from(
            ExternalAccount externalAccount
    ) {
        return new ExternalAccountSettingsResponse(
                externalAccount.getProvider(),
                true,
                externalAccount.isUseEnabled()
        );
    }
}
