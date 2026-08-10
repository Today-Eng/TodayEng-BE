package com.example.todayEng.domain.user.service;

import com.example.todayEng.domain.user.client.OAuthProviderClientRegistry;
import com.example.todayEng.domain.user.dto.oauth.OAuthTokenResponse;
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.example.todayEng.global.log.ExternalCallLog;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

/**
 * 만료가 임박한 Access Token은 미리 갱신하고, 그래도 401이 돌아오면 한 번 더 갱신해 재시도한다.
 * Provider가 통보 없이 토큰을 무효화하는 경우가 있어 만료 시각만 믿을 수 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalAccountTokenService {

    private static final Duration EXPIRY_SKEW = Duration.ofMinutes(1);

    private final OAuthProviderClientRegistry providerClientRegistry;
    private final ExternalAccountRepository externalAccountRepository;
    private final Clock clock;

    public <T> T callWithAccessToken(
            ExternalAccount account,
            Function<String, T> call
    ) {
        String accessToken = expiresSoon(account)
                ? refreshAccessToken(account)
                : account.getAccessToken();

        try {
            return call.apply(accessToken);
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() != HttpStatus.UNAUTHORIZED) {
                throw exception;
            }
            log.info("External access token rejected, retrying after refresh: "
                            + "provider={}, externalAccountId={}",
                    account.getProvider(), account.getId());
            return call.apply(refreshAccessToken(account));
        }
    }

    private boolean expiresSoon(ExternalAccount account) {
        LocalDateTime expiresAt = account.getTokenExpiresAt();
        // 만료 시각을 모르는 계정은 실제 401이 날 때까지 기존 토큰을 그대로 쓴다
        return expiresAt != null
                && !expiresAt.isAfter(LocalDateTime.now(clock).plus(EXPIRY_SKEW));
    }

    /**
     * 갱신 실패는 사용자가 외부 서비스에서 권한을 취소했을 가능성이 높다. 연동 상태를 임의로
     * 바꾸지 않고 예외를 던져 해당 컨텍스트 수집만 실패로 남긴다.
     */
    public String refreshAccessToken(ExternalAccount account) {
        OAuthTokenResponse tokens;
        try {
            tokens = providerClientRegistry.getClient(account.getProvider())
                    .refreshAccessToken(account.getRefreshToken());
        } catch (BaseException exception) {
            log.warn("External access token refresh failed: provider={}, "
                            + "externalAccountId={}, errorCode={}",
                    account.getProvider(), account.getId(),
                    exception.getErrorCode().name());
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("External access token refresh failed: provider={}, "
                            + "externalAccountId={}, cause={}",
                    account.getProvider(), account.getId(),
                    ExternalCallLog.describe(exception));
            throw new BaseException(ErrorCode.OAUTH_TOKEN_REFRESH_FAILED);
        }

        account.updateTokens(
                tokens.accessToken(),
                tokens.refreshToken(),
                resolveExpiresAt(tokens.expiresInSeconds())
        );
        // 수집 경로는 트랜잭션 밖에서 계정을 조회하므로 변경 감지에 기댈 수 없다
        externalAccountRepository.save(account);

        log.info("External access token refreshed: provider={}, externalAccountId={}",
                account.getProvider(), account.getId());
        return account.getAccessToken();
    }

    private LocalDateTime resolveExpiresAt(Long expiresInSeconds) {
        if (expiresInSeconds == null || expiresInSeconds <= 0) {
            return null;
        }
        return LocalDateTime.now(clock).plusSeconds(expiresInSeconds);
    }
}
