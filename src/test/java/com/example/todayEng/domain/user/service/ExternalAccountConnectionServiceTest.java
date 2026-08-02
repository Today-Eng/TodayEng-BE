package com.example.todayEng.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.user.dto.oauth.ExternalUserInfo;
import com.example.todayEng.domain.user.dto.oauth.OAuthTokenResponse;
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalAccountConnectionServiceTest {

    @Mock
    private ExternalAccountRepository externalAccountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExternalAccountConnectionService service;

    @ParameterizedTest
    @EnumSource(ExternalServiceProvider.class)
    void saveOrUpdate_createsAccountForProvider(
            ExternalServiceProvider provider
    ) {
        Long userId = 1L;
        User user = User.create();
        OAuthTokenResponse tokenResponse =
                new OAuthTokenResponse(
                        "access-token",
                        "refresh-token",
                        3600L
                );
        ExternalUserInfo userInfo =
                new ExternalUserInfo(
                        "provider-account-id",
                        "account@example.com"
                );

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));
        given(externalAccountRepository
                .findByProviderAndProviderAccountId(
                        provider,
                        userInfo.providerAccountId()
                ))
                .willReturn(Optional.empty());
        given(externalAccountRepository.findByUserAndProvider(
                user,
                provider
        )).willReturn(Optional.empty());

        service.saveOrUpdate(
                userId,
                provider,
                tokenResponse,
                userInfo
        );

        ArgumentCaptor<ExternalAccount> accountCaptor =
                ArgumentCaptor.forClass(ExternalAccount.class);
        verify(externalAccountRepository).save(
                accountCaptor.capture()
        );

        ExternalAccount savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getProvider()).isEqualTo(provider);
        assertThat(savedAccount.getProviderAccountId())
                .isEqualTo(userInfo.providerAccountId());
        assertThat(savedAccount.getAccessToken())
                .isEqualTo(tokenResponse.accessToken());
        assertThat(savedAccount.getRefreshToken())
                .isEqualTo(tokenResponse.refreshToken());
        assertThat(savedAccount.isUseEnabled()).isTrue();
    }
}
