package com.example.todayEng.domain.user.service;

import com.example.todayEng.domain.user.client.OAuthProviderClient;
import com.example.todayEng.domain.user.client.OAuthProviderClientRegistry;
import com.example.todayEng.domain.user.dto.oauth.ExternalUserInfo;
import com.example.todayEng.domain.user.dto.oauth.OAuthTokenResponse;
import com.example.todayEng.domain.user.dto.response.OAuthAuthorizationResponse;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExternalAccountOAuthService {

    private final OAuthAuthorizationRequestService
            oauthAuthorizationRequestService;

    private final OAuthProviderClientRegistry
            oauthProviderClientRegistry;

    private final ExternalAccountConnectionService
            externalAccountConnectionService;

    public OAuthAuthorizationResponse createAuthorizationUrl(
            Long userId,
            ExternalServiceProvider provider
    ) {
        String state = oauthAuthorizationRequestService.issue(
                userId,
                provider
        );

        OAuthProviderClient providerClient =
                oauthProviderClientRegistry.getClient(provider);

        String authorizationUrl =
                providerClient.buildAuthorizationUrl(state);

        return new OAuthAuthorizationResponse(
                authorizationUrl
        );
    }

    public void connectExternalAccount(
            ExternalServiceProvider provider,
            String code,
            String state
    ) {
        validateAuthorizationCode(code);

        Long userId =
                oauthAuthorizationRequestService
                        .validateAndConsume(
                                state,
                                provider
                        );

        OAuthProviderClient providerClient =
                oauthProviderClientRegistry.getClient(provider);

        OAuthTokenResponse tokenResponse =
                providerClient.exchangeToken(code);

        ExternalUserInfo externalUserInfo =
                providerClient.fetchUserInfo(
                        tokenResponse.accessToken()
                );

        externalAccountConnectionService.saveOrUpdate(
                userId,
                provider,
                tokenResponse,
                externalUserInfo
        );
    }

    private void validateAuthorizationCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BaseException(
                    ErrorCode.OAUTH_AUTHORIZATION_CODE_MISSING
            );
        }
    }
}