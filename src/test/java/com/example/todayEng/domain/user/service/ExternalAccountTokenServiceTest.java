package com.example.todayEng.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.user.client.OAuthProviderClient;
import com.example.todayEng.domain.user.client.OAuthProviderClientRegistry;
import com.example.todayEng.domain.user.dto.oauth.OAuthTokenResponse;
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@ExtendWith(MockitoExtension.class)
class ExternalAccountTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");

    @Mock OAuthProviderClientRegistry registry;
    @Mock ExternalAccountRepository accountRepository;
    @Mock OAuthProviderClient providerClient;

    private Clock clock;
    private ExternalAccountTokenService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"));
        service = new ExternalAccountTokenService(registry, accountRepository, clock);
    }

    @Test
    @DisplayName("만료가 남은 토큰은 갱신 없이 그대로 사용한다")
    void usesStoredTokenWhenNotExpiring() {
        ExternalAccount account = account("stored-token", LocalDateTime.now(clock).plusHours(1));

        String used = service.callWithAccessToken(account, token -> token);

        assertThat(used).isEqualTo("stored-token");
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("만료가 임박한 토큰은 호출 전에 미리 갱신한다")
    void refreshesBeforeCallWhenTokenExpiresSoon() {
        ExternalAccount account = account("old-token", LocalDateTime.now(clock).plusSeconds(30));
        givenRefreshReturns("new-token", "new-refresh", 3600L);

        String used = service.callWithAccessToken(account, token -> token);

        assertThat(used).isEqualTo("new-token");
        assertThat(account.getAccessToken()).isEqualTo("new-token");
        assertThat(account.getRefreshToken()).isEqualTo("new-refresh");
        assertThat(account.getTokenExpiresAt())
                .isEqualTo(LocalDateTime.now(clock).plusSeconds(3600));
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("만료 시각을 모르는 계정은 저장된 토큰으로 먼저 호출한다")
    void usesStoredTokenWhenExpiryIsUnknown() {
        ExternalAccount account = account("stored-token", null);

        String used = service.callWithAccessToken(account, token -> token);

        assertThat(used).isEqualTo("stored-token");
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("401이 오면 토큰을 갱신해 한 번만 재시도한다")
    void refreshesAndRetriesOnceOnUnauthorized() {
        ExternalAccount account = account("old-token", LocalDateTime.now(clock).plusHours(1));
        givenRefreshReturns("new-token", null, 3600L);
        AtomicInteger attempts = new AtomicInteger();

        String used = service.callWithAccessToken(account, token -> {
            if (attempts.incrementAndGet() == 1) {
                throw unauthorized();
            }
            return token;
        });

        assertThat(used).isEqualTo("new-token");
        assertThat(attempts.get()).isEqualTo(2);
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("갱신 응답에 Refresh Token이 없으면 기존 값을 유지한다")
    void keepsExistingRefreshTokenWhenProviderDoesNotRotate() {
        ExternalAccount account = account("old-token", LocalDateTime.now(clock).plusSeconds(10));
        givenRefreshReturns("new-token", null, 3600L);

        service.callWithAccessToken(account, token -> token);

        assertThat(account.getRefreshToken()).isEqualTo("stored-refresh");
    }

    @Test
    @DisplayName("401이 아닌 오류는 갱신하지 않고 그대로 전파한다")
    void doesNotRefreshOnNonUnauthorizedFailure() {
        ExternalAccount account = account("stored-token", LocalDateTime.now(clock).plusHours(1));

        assertThatThrownBy(() -> service.callWithAccessToken(account, token -> {
            throw HttpServerErrorException.create(
                    HttpStatus.INTERNAL_SERVER_ERROR, "boom", null, null, null);
        })).isInstanceOf(HttpServerErrorException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("갱신에 실패하면 연동 상태를 바꾸지 않고 예외를 올린다")
    void propagatesRefreshFailureWithoutChangingConnection() {
        ExternalAccount account = account("old-token", LocalDateTime.now(clock).plusSeconds(10));
        given(registry.getClient(ExternalServiceProvider.SPOTIFY)).willReturn(providerClient);
        given(providerClient.refreshAccessToken("stored-refresh"))
                .willThrow(new BaseException(ErrorCode.OAUTH_TOKEN_REFRESH_FAILED));

        assertThatThrownBy(() -> service.callWithAccessToken(account, token -> token))
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.OAUTH_TOKEN_REFRESH_FAILED));

        assertThat(account.isUseEnabled()).isTrue();
        assertThat(account.getAccessToken()).isEqualTo("old-token");
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("재시도한 요청도 401이면 예외를 전파한다")
    void propagatesUnauthorizedWhenRetryAlsoFails() {
        ExternalAccount account = account("old-token", LocalDateTime.now(clock).plusHours(1));
        givenRefreshReturns("new-token", null, 3600L);

        assertThatThrownBy(() -> service.callWithAccessToken(account, token -> {
            throw unauthorized();
        })).isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    private void givenRefreshReturns(String accessToken, String refreshToken, Long expiresIn) {
        given(registry.getClient(ExternalServiceProvider.SPOTIFY)).willReturn(providerClient);
        given(providerClient.refreshAccessToken("stored-refresh"))
                .willReturn(new OAuthTokenResponse(accessToken, refreshToken, expiresIn));
    }

    private HttpClientErrorException unauthorized() {
        return HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null);
    }

    private ExternalAccount account(String accessToken, LocalDateTime expiresAt) {
        return ExternalAccount.create(
                User.create(),
                ExternalServiceProvider.SPOTIFY,
                "provider-account-id",
                "user@example.com",
                accessToken,
                "stored-refresh",
                expiresAt
        );
    }
}
