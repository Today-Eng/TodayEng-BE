package com.example.todayEng.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import com.example.todayEng.domain.user.client.OAuthProviderClientRegistry;
import com.example.todayEng.domain.user.client.OAuthProviderClient;
import com.example.todayEng.domain.user.dto.oauth.ExternalUserInfo;
import com.example.todayEng.domain.user.dto.oauth.OAuthTokenResponse;
import com.example.todayEng.domain.user.entity.enums.OAuthCallbackFailureStage;
import com.example.todayEng.domain.user.entity.enums.OAuthCallbackFailureType;
import com.example.todayEng.domain.user.service.OAuthAuthorizationRequestService.ProcessingClaim;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ExternalAccountOAuthServiceTest {

    @Mock
    private OAuthAuthorizationRequestService
            oauthAuthorizationRequestService;

    @Mock
    private OAuthProviderClientRegistry
            oauthProviderClientRegistry;

    @Mock
    private ExternalAccountConnectionService
            externalAccountConnectionService;

    @Mock
    private OAuthProviderClient providerClient;

    @InjectMocks
    private ExternalAccountOAuthService service;

    @Test
    void createAuthorizationUrl_unsupportedProvider_doesNotIssueState() {
        ExternalServiceProvider provider =
                ExternalServiceProvider.SPOTIFY;
        given(oauthProviderClientRegistry.getClient(provider))
                .willThrow(new BaseException(
                        ErrorCode.UNSUPPORTED_OAUTH_PROVIDER
                ));

        assertThatThrownBy(() ->
                service.createAuthorizationUrl(1L, provider)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);

        verify(oauthAuthorizationRequestService, never())
                .issue(1L, provider);
    }

    @Test
    void connectExternalAccount_marksRequestSucceededAfterAccountSave() {
        ExternalServiceProvider provider = ExternalServiceProvider.SPOTIFY;
        OAuthTokenResponse token = new OAuthTokenResponse("access", "refresh", 3600L);
        ExternalUserInfo userInfo = new ExternalUserInfo("provider-id", "user@example.com");
        given(oauthAuthorizationRequestService.startProcessing("state", provider))
                .willReturn(new ProcessingClaim(10L, 1L));
        given(oauthProviderClientRegistry.getClient(provider)).willReturn(providerClient);
        given(providerClient.exchangeToken("code")).willReturn(token);
        given(providerClient.fetchUserInfo("access")).willReturn(userInfo);

        service.connectExternalAccount(provider, "code", "state", null);

        verify(externalAccountConnectionService)
                .saveOrUpdate(1L, provider, token, userInfo);
        verify(oauthAuthorizationRequestService).succeed(10L);
    }

    @Test
    void connectExternalAccount_recordsTokenExchangeFailure() {
        ExternalServiceProvider provider = ExternalServiceProvider.SPOTIFY;
        given(oauthAuthorizationRequestService.startProcessing("state", provider))
                .willReturn(new ProcessingClaim(10L, 1L));
        given(oauthProviderClientRegistry.getClient(provider)).willReturn(providerClient);
        given(providerClient.exchangeToken("code"))
                .willThrow(new BaseException(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED));

        assertThatThrownBy(() ->
                service.connectExternalAccount(provider, "code", "state", null))
                .isInstanceOf(BaseException.class);

        verify(oauthAuthorizationRequestService).fail(
                10L, OAuthCallbackFailureStage.TOKEN_EXCHANGE,
                OAuthCallbackFailureType.EXTERNAL_API);
        verify(oauthAuthorizationRequestService, never()).succeed(10L);
    }

    @Test
    void connectExternalAccount_recordsDatabaseFailureWithoutSensitiveValues() {
        ExternalServiceProvider provider = ExternalServiceProvider.SPOTIFY;
        OAuthTokenResponse token =
                new OAuthTokenResponse("sensitive-access", "sensitive-refresh", 3600L);
        ExternalUserInfo userInfo = new ExternalUserInfo("provider-id", "user@example.com");
        given(oauthAuthorizationRequestService.startProcessing("state", provider))
                .willReturn(new ProcessingClaim(10L, 1L));
        given(oauthProviderClientRegistry.getClient(provider)).willReturn(providerClient);
        given(providerClient.exchangeToken("sensitive-code")).willReturn(token);
        given(providerClient.fetchUserInfo("sensitive-access")).willReturn(userInfo);
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(externalAccountConnectionService)
                .saveOrUpdate(1L, provider, token, userInfo);

        assertThatThrownBy(() -> service.connectExternalAccount(
                provider, "sensitive-code", "state", null))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(oauthAuthorizationRequestService).fail(
                10L, OAuthCallbackFailureStage.ACCOUNT_SAVE,
                OAuthCallbackFailureType.DATABASE);
    }

    @Test
    void deniedCallbackIsClaimedThenMarkedFailed() {
        ExternalServiceProvider provider = ExternalServiceProvider.SPOTIFY;
        given(oauthAuthorizationRequestService.startProcessing("state", provider))
                .willReturn(new ProcessingClaim(10L, 1L));

        assertThatThrownBy(() ->
                service.connectExternalAccount(provider, null, "state", "access_denied"))
                .isInstanceOf(BaseException.class);

        verify(oauthAuthorizationRequestService).fail(
                10L, OAuthCallbackFailureStage.CALLBACK_VALIDATION,
                OAuthCallbackFailureType.AUTHORIZATION_DENIED);
        verify(oauthProviderClientRegistry, never()).getClient(provider);
    }
}
