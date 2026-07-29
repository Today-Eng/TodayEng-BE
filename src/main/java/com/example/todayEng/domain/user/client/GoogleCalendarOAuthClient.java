package com.example.todayEng.domain.user.client;

import com.example.todayEng.domain.user.config.GoogleOAuthProperties;
import com.example.todayEng.domain.user.dto.google.GoogleOAuthTokenResponse;
import com.example.todayEng.domain.user.dto.google.GoogleUserInfoResponse;
import com.example.todayEng.domain.user.dto.oauth.ExternalUserInfo;
import com.example.todayEng.domain.user.dto.oauth.OAuthTokenResponse;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class GoogleCalendarOAuthClient implements OAuthProviderClient {

    private static final String AUTHORIZATION_URI =
            "https://accounts.google.com/o/oauth2/v2/auth";

    private static final String TOKEN_URI =
            "https://oauth2.googleapis.com/token";

    private static final String USER_INFO_URI =
            "https://openidconnect.googleapis.com/v1/userinfo";

    private static final String RESPONSE_TYPE = "code";

    private static final String GRANT_TYPE =
            "authorization_code";

    private static final String ACCESS_TYPE = "offline";

    private static final String PROMPT = "consent";

    private static final String SCOPE = String.join(
            " ",
            "openid",
            "email",
            "https://www.googleapis.com/auth/calendar.readonly"
    );

    private final RestClient restClient;

    private final GoogleOAuthProperties googleOAuthProperties;

    @Override
    public ExternalServiceProvider supports() {
        return ExternalServiceProvider.GOOGLE_CALENDAR;
    }

    @Override
    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder
                .fromUriString(AUTHORIZATION_URI)
                .queryParam(
                        "client_id",
                        googleOAuthProperties.clientId()
                )
                .queryParam(
                        "redirect_uri",
                        googleOAuthProperties.redirectUri()
                )
                .queryParam(
                        "response_type",
                        RESPONSE_TYPE
                )
                .queryParam(
                        "scope",
                        SCOPE
                )
                .queryParam(
                        "access_type",
                        ACCESS_TYPE
                )
                .queryParam(
                        "prompt",
                        PROMPT
                )
                .queryParam(
                        "state",
                        state
                )
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public OAuthTokenResponse exchangeToken(String code) {
        MultiValueMap<String, String> requestBody =
                new LinkedMultiValueMap<>();

        requestBody.add(
                "client_id",
                googleOAuthProperties.clientId()
        );
        requestBody.add(
                "client_secret",
                googleOAuthProperties.clientSecret()
        );
        requestBody.add(
                "code",
                code
        );
        requestBody.add(
                "grant_type",
                GRANT_TYPE
        );
        requestBody.add(
                "redirect_uri",
                googleOAuthProperties.redirectUri()
        );

        try {
            GoogleOAuthTokenResponse response = restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(
                            MediaType.APPLICATION_FORM_URLENCODED
                    )
                    .body(requestBody)
                    .retrieve()
                    .body(GoogleOAuthTokenResponse.class);

            if (response == null
                    || response.accessToken() == null
                    || response.accessToken().isBlank()) {
                throw new BaseException(
                        ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED
                );
            }

            return new OAuthTokenResponse(
                    response.accessToken(),
                    response.refreshToken(),
                    response.expiresIn()
            );

        } catch (BaseException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new BaseException(
                    ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED
            );
        }
    }

    @Override
    public ExternalUserInfo fetchUserInfo(String accessToken) {
        try {
            GoogleUserInfoResponse response = restClient.get()
                    .uri(USER_INFO_URI)
                    .headers(headers ->
                            headers.setBearerAuth(accessToken)
                    )
                    .retrieve()
                    .body(GoogleUserInfoResponse.class);

            if (response == null
                    || response.sub() == null
                    || response.sub().isBlank()) {
                throw new BaseException(
                        ErrorCode.OAUTH_USER_INFO_FAILED
                );
            }

            return new ExternalUserInfo(
                    response.sub(),
                    response.email()
            );

        } catch (BaseException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new BaseException(
                    ErrorCode.OAUTH_USER_INFO_FAILED
            );
        }
    }
}