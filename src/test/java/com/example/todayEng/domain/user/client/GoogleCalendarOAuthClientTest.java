package com.example.todayEng.domain.user.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.user.config.GoogleOAuthProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GoogleCalendarOAuthClientTest {

    @Test
    void buildAuthorizationUrl_usesConfiguredUrisAndScopes() {
        GoogleOAuthProperties properties =
                new GoogleOAuthProperties(
                        "client-id",
                        "client-secret",
                        "https://api.example.com/oauth/callback",
                        "https://provider.example.com/authorize",
                        "https://provider.example.com/token",
                        "https://provider.example.com/userinfo",
                        List.of(
                                "openid",
                                "email",
                                "calendar.readonly"
                        )
                );
        GoogleCalendarOAuthClient client =
                new GoogleCalendarOAuthClient(
                        RestClient.create(),
                        properties
                );

        String authorizationUrl =
                client.buildAuthorizationUrl("state-value");

        assertThat(authorizationUrl)
                .startsWith(
                        "https://provider.example.com/authorize?"
                )
                .contains("client_id=client-id")
                .contains(
                        "redirect_uri=https://api.example.com/oauth/callback"
                )
                .contains("scope=openid%20email%20calendar.readonly")
                .contains("state=state-value");
    }
}
