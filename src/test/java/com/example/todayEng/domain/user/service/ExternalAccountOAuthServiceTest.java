package com.example.todayEng.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.user.client.OAuthProviderClientRegistry;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
