package com.example.todayEng.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ExternalAccountTest {

    @Test
    void reconnect_sameAccount_preservesExistingRefreshToken() {
        ExternalAccount account = ExternalAccount.create(
                User.create(),
                ExternalServiceProvider.GOOGLE_CALENDAR,
                "provider-account-id",
                "old@example.com",
                "old-access-token",
                "old-refresh-token",
                LocalDateTime.now().plusHours(1)
        );
        LocalDateTime newExpiresAt =
                LocalDateTime.now().plusHours(2);

        account.reconnect(
                "provider-account-id",
                "new@example.com",
                "new-access-token",
                null,
                newExpiresAt
        );

        assertThat(account.getAccountIdentifier())
                .isEqualTo("new@example.com");
        assertThat(account.getAccessToken())
                .isEqualTo("new-access-token");
        assertThat(account.getRefreshToken())
                .isEqualTo("old-refresh-token");
        assertThat(account.getTokenExpiresAt())
                .isEqualTo(newExpiresAt);
        assertThat(account.isUseEnabled()).isTrue();
    }
}
