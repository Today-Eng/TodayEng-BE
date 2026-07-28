package com.example.todayEng.domain.user.dto.response;

import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import java.time.LocalDateTime;

public record ExternalAccountResponse(
        Long externalAccountId,
        ExternalServiceProvider provider,
        String accountIdentifier,
        boolean useEnabled,
        LocalDateTime connectedAt
) {

    public static ExternalAccountResponse from(ExternalAccount externalAccount) {
        return new ExternalAccountResponse(
                externalAccount.getId(),
                externalAccount.getProvider(),
                externalAccount.getAccountIdentifier(),
                externalAccount.isUseEnabled(),
                externalAccount.getConnectedAt()
        );
    }
}
