package com.example.todayEng.domain.user.service;

import com.example.todayEng.domain.user.dto.oauth.ExternalUserInfo;
import com.example.todayEng.domain.user.dto.oauth.OAuthTokenResponse;
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.domain.user.repository.UserRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExternalAccountConnectionService {

    private final ExternalAccountRepository
            externalAccountRepository;

    private final UserRepository userRepository;

    @Transactional
    public void saveOrUpdate(
            Long userId,
            ExternalServiceProvider provider,
            OAuthTokenResponse tokenResponse,
            ExternalUserInfo externalUserInfo
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.USER_NOT_FOUND
                ));

        validateProviderAccountOwnership(
                userId,
                provider,
                externalUserInfo.providerAccountId()
        );

        LocalDateTime tokenExpiresAt =
                calculateTokenExpiresAt(
                        tokenResponse.expiresInSeconds()
                );

        ExternalAccount externalAccount =
                externalAccountRepository
                        .findByUserAndProvider(
                                user,
                                provider
                        )
                        .orElse(null);

        if (externalAccount == null) {
            externalAccount = createExternalAccount(
                    user,
                    provider,
                    tokenResponse,
                    externalUserInfo,
                    tokenExpiresAt
            );
        } else {
            externalAccount.reconnect(
                    externalUserInfo.providerAccountId(),
                    externalUserInfo.accountIdentifier(),
                    tokenResponse.accessToken(),
                    tokenResponse.refreshToken(),
                    tokenExpiresAt
            );
        }

        externalAccountRepository.save(externalAccount);
    }

    private void validateProviderAccountOwnership(
            Long userId,
            ExternalServiceProvider provider,
            String providerAccountId
    ) {
        externalAccountRepository
                .findByProviderAndProviderAccountId(
                        provider,
                        providerAccountId
                )
                .filter(account ->
                        !account.getUser().getId().equals(userId)
                )
                .ifPresent(account -> {
                    throw new BaseException(
                            ErrorCode.EXTERNAL_ACCOUNT_ALREADY_LINKED
                    );
                });
    }

    private ExternalAccount createExternalAccount(
            User user,
            ExternalServiceProvider provider,
            OAuthTokenResponse tokenResponse,
            ExternalUserInfo externalUserInfo,
            LocalDateTime tokenExpiresAt
    ) {
        return ExternalAccount.create(
                user,
                provider,
                externalUserInfo.providerAccountId(),
                externalUserInfo.accountIdentifier(),
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenExpiresAt
        );
    }

    private LocalDateTime calculateTokenExpiresAt(
            Long expiresIn
    ) {
        if (expiresIn == null) {
            return null;
        }

        return LocalDateTime.now()
                .plusSeconds(expiresIn);
    }
}
