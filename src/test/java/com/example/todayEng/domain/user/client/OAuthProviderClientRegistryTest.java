package com.example.todayEng.domain.user.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.List;
import org.junit.jupiter.api.Test;

class OAuthProviderClientRegistryTest {

    @Test
    void getClient_returnsMatchingClient() {
        OAuthProviderClient googleClient = providerClient(
                ExternalServiceProvider.GOOGLE_CALENDAR
        );
        OAuthProviderClient spotifyClient = providerClient(
                ExternalServiceProvider.SPOTIFY
        );
        OAuthProviderClientRegistry registry =
                new OAuthProviderClientRegistry(
                        List.of(googleClient, spotifyClient)
                );

        assertThat(registry.getClient(
                ExternalServiceProvider.GOOGLE_CALENDAR
        )).isSameAs(googleClient);
        assertThat(registry.getClient(
                ExternalServiceProvider.SPOTIFY
        )).isSameAs(spotifyClient);
    }

    @Test
    void constructor_duplicateProvider_throws() {
        OAuthProviderClient firstClient = providerClient(
                ExternalServiceProvider.GOOGLE_CALENDAR
        );
        OAuthProviderClient secondClient = providerClient(
                ExternalServiceProvider.GOOGLE_CALENDAR
        );

        assertThatThrownBy(() ->
                new OAuthProviderClientRegistry(
                        List.of(firstClient, secondClient)
                )
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getClient_unregisteredProvider_throws() {
        OAuthProviderClient googleClient = providerClient(
                ExternalServiceProvider.GOOGLE_CALENDAR
        );
        OAuthProviderClientRegistry registry =
                new OAuthProviderClientRegistry(
                        List.of(googleClient)
                );

        assertThatThrownBy(() ->
                registry.getClient(
                        ExternalServiceProvider.SPOTIFY
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }

    private OAuthProviderClient providerClient(
            ExternalServiceProvider provider
    ) {
        OAuthProviderClient client =
                mock(OAuthProviderClient.class);
        given(client.supports()).willReturn(provider);
        return client;
    }
}
