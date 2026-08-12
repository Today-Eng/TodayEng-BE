package com.example.todayEng.domain.user.service;

import com.example.todayEng.domain.user.client.OAuthProviderClient;
import com.example.todayEng.domain.user.client.OAuthProviderClientRegistry;
import com.example.todayEng.domain.user.dto.oauth.ExternalUserInfo;
import com.example.todayEng.domain.user.dto.oauth.OAuthTokenResponse;
import com.example.todayEng.domain.user.dto.response.OAuthAuthorizationResponse;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.entity.enums.OAuthCallbackFailureStage;
import com.example.todayEng.domain.user.entity.enums.OAuthCallbackFailureType;
import com.example.todayEng.domain.user.service.OAuthAuthorizationRequestService.ProcessingClaim;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;

@Service
@RequiredArgsConstructor
public class ExternalAccountOAuthService {

    private final OAuthAuthorizationRequestService
            oauthAuthorizationRequestService;

    private final OAuthProviderClientRegistry
            oauthProviderClientRegistry;

    private final OAuthCallbackCompletionService oauthCallbackCompletionService;

    public OAuthAuthorizationResponse createAuthorizationUrl(
            Long userId,
            ExternalServiceProvider provider
    ) {
        OAuthProviderClient providerClient =
                oauthProviderClientRegistry.getClient(provider);

        String state = oauthAuthorizationRequestService.issue(
                userId,
                provider
        );

        String authorizationUrl =
                providerClient.buildAuthorizationUrl(state);

        return new OAuthAuthorizationResponse(
                authorizationUrl
        );
    }

    public void connectExternalAccount(
            ExternalServiceProvider provider,
            String code,
            String state,
            String error
    ) {
        ProcessingClaim claim =
                oauthAuthorizationRequestService
                        .startProcessing(
                                state,
                                provider
                        );
        OAuthCallbackFailureStage stage = OAuthCallbackFailureStage.CALLBACK_VALIDATION;
        try {
            validateOAuthCallback(code, error);
            OAuthProviderClient providerClient = oauthProviderClientRegistry.getClient(provider);

            stage = OAuthCallbackFailureStage.TOKEN_EXCHANGE;
            OAuthTokenResponse tokenResponse = providerClient.exchangeToken(code);

            stage = OAuthCallbackFailureStage.USER_INFO;
            ExternalUserInfo externalUserInfo =
                    providerClient.fetchUserInfo(tokenResponse.accessToken());

            stage = OAuthCallbackFailureStage.ACCOUNT_SAVE;
            oauthCallbackCompletionService.saveAccountAndSucceed(
                    claim.requestId(), claim.userId(), provider,
                    tokenResponse, externalUserInfo);
        } catch (RuntimeException exception) {
            oauthAuthorizationRequestService.fail(
                    claim.requestId(), stage, classifyFailure(exception));
            throw exception;
        }
    }

    private OAuthCallbackFailureType classifyFailure(RuntimeException exception) {
        if (exception instanceof BaseException baseException) {
            return switch (baseException.getErrorCode()) {
                case OAUTH_AUTHORIZATION_DENIED ->
                        OAuthCallbackFailureType.AUTHORIZATION_DENIED;
                case OAUTH_AUTHORIZATION_CODE_MISSING ->
                        OAuthCallbackFailureType.AUTHORIZATION_CODE_MISSING;
                case EXTERNAL_ACCOUNT_ALREADY_LINKED ->
                        OAuthCallbackFailureType.CONFLICT;
                case OAUTH_TOKEN_EXCHANGE_FAILED, OAUTH_USER_INFO_FAILED, EXTERNAL_API_ERROR ->
                        OAuthCallbackFailureType.EXTERNAL_API;
                default -> OAuthCallbackFailureType.INTERNAL;
            };
        }
        if (exception instanceof DataAccessException) {
            return OAuthCallbackFailureType.DATABASE;
        }
        return OAuthCallbackFailureType.INTERNAL;
    }

    private void validateOAuthCallback(
            String code,
            String error
    ) {
        if (error != null && !error.isBlank()) {
            throw new BaseException(
                    ErrorCode.OAUTH_AUTHORIZATION_DENIED
            );
        }

        if (code == null || code.isBlank()) {
            throw new BaseException(
                    ErrorCode.OAUTH_AUTHORIZATION_CODE_MISSING
            );
        }
    }
}
