package com.example.todayEng.domain.user.dto.response;

import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ExternalAccountResponse(
        @Schema(description = "연동된 경우에만 값이 존재합니다.", example = "1")
        Long externalAccountId,

        @Schema(description = "외부 서비스 Provider", example = "GOOGLE_CALENDAR")
        ExternalServiceProvider provider,

        @Schema(description = "계정 연동 여부", example = "true")
        boolean connected,

        @Schema(description = "회고 질문 생성에 이 서비스 데이터를 사용할지 여부", example = "true")
        boolean useEnabled,

        @Schema(description = "연동 관리 화면에 표시할 계정 정보", example = "example@gmail.com")
        String accountIdentifier,

        @Schema(description = "연동 시각", example = "2026-07-24T14:30:00")
        LocalDateTime connectedAt
) {

    public static ExternalAccountResponse from(ExternalAccount externalAccount) {
        return new ExternalAccountResponse(
                externalAccount.getId(),
                externalAccount.getProvider(),
                true,
                externalAccount.isUseEnabled(),
                externalAccount.getAccountIdentifier(),
                externalAccount.getConnectedAt()
        );
    }

    public static ExternalAccountResponse disconnected(
            ExternalServiceProvider provider
    ) {
        return new ExternalAccountResponse(
                null,
                provider,
                false,
                false,
                null,
                null
        );
    }
}
