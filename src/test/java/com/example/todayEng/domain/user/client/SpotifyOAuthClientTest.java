package com.example.todayEng.domain.user.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.todayEng.domain.user.config.SpotifyOAuthProperties;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(OutputCaptureExtension.class)
class SpotifyOAuthClientTest {

    private static final String TOKEN_URI =
            "https://provider.example.com/token";
    private static final String USER_INFO_URI =
            "https://provider.example.com/me";

    private MockRestServiceServer server;
    private SpotifyOAuthClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder =
                RestClient.builder();
        server = MockRestServiceServer.bindTo(
                restClientBuilder
        ).build();
        client = new SpotifyOAuthClient(
                restClientBuilder.build(),
                properties(),
                new ObjectMapper()
        );
    }

    @Test
    void supports_returnsSpotify() {
        assertThat(client.supports())
                .isEqualTo(ExternalServiceProvider.SPOTIFY);
    }

    @Test
    void buildAuthorizationUrl_usesConfiguredValues() {
        String authorizationUrl =
                client.buildAuthorizationUrl("state-value");

        assertThat(authorizationUrl)
                .startsWith(
                        "https://provider.example.com/authorize?"
                )
                .contains("client_id=client-id")
                .contains("response_type=code")
                .contains(
                        "redirect_uri=https://api.example.com/oauth/callback"
                )
                .contains(
                        "scope=user-read-email%20user-read-recently-played"
                )
                .contains("state=state-value");
    }

    @Test
    void exchangeToken_usesBasicAuthentication() {
        server.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ="
                ))
                .andExpect(content().string(
                        containsString(
                                "grant_type=authorization_code"
                        )
                ))
                .andExpect(content().string(
                        containsString("code=authorization-code")
                ))
                .andRespond(withSuccess(
                        """
                        {
                          "access_token": "access-token",
                          "refresh_token": "refresh-token",
                          "expires_in": 3600,
                          "token_type": "Bearer",
                          "scope": "user-read-email"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        var response =
                client.exchangeToken("authorization-code");

        assertThat(response.accessToken())
                .isEqualTo("access-token");
        assertThat(response.refreshToken())
                .isEqualTo("refresh-token");
        assertThat(response.expiresInSeconds())
                .isEqualTo(3600L);
        server.verify();
    }

    @Test
    void exchangeToken_providerError_logsSafeErrorDetails(
            CapturedOutput output
    ) {
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "error": "invalid_grant",
                                  "error_description": "Invalid authorization code",
                                  "access_token": "must-not-be-logged"
                                }
                                """));

        assertThatThrownBy(() ->
                client.exchangeToken("authorization-code")
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED);

        assertThat(output.getOut())
                .contains(
                        "status=400",
                        "error=invalid_grant",
                        "description=Invalid authorization code"
                )
                .doesNotContain(
                        "must-not-be-logged",
                        "authorization-code"
                );
        server.verify();
    }

    @Test
    void fetchUserInfo_usesStableAccountId() {
        server.expect(requestTo(USER_INFO_URI))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer access-token"
                ))
                .andRespond(withSuccess(
                        """
                        {
                          "account_id": "stable-account-id",
                          "id": "spotify-user-id",
                          "email": "spotify@example.com",
                          "display_name": "Spotify User"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        var response =
                client.fetchUserInfo("access-token");

        assertThat(response.providerAccountId())
                .isEqualTo("stable-account-id");
        assertThat(response.accountIdentifier())
                .isEqualTo("spotify@example.com");
        server.verify();
    }

    @Test
    void fetchUserInfo_missingAccountId_throws() {
        server.expect(requestTo(USER_INFO_URI))
                .andRespond(withSuccess(
                        """
                        {
                          "id": "spotify-user-id",
                          "display_name": "Spotify User"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() ->
                client.fetchUserInfo("access-token")
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.OAUTH_USER_INFO_FAILED);
        server.verify();
    }

    private SpotifyOAuthProperties properties() {
        return new SpotifyOAuthProperties(
                "client-id",
                "client-secret",
                "https://api.example.com/oauth/callback",
                "https://provider.example.com/authorize",
                TOKEN_URI,
                USER_INFO_URI,
                List.of(
                        "user-read-email",
                        "user-read-recently-played"
                )
        );
    }
}
