package com.example.todayEng.domain.user.client;

import com.example.todayEng.domain.user.config.SpotifyOAuthProperties;
import com.example.todayEng.domain.user.dto.oauth.ExternalUserInfo;
import com.example.todayEng.domain.user.dto.oauth.OAuthTokenResponse;
import com.example.todayEng.domain.user.dto.spotify.SpotifyOAuthTokenResponse;
import com.example.todayEng.domain.user.dto.spotify.SpotifyUserInfoResponse;
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
public class SpotifyOAuthClient implements OAuthProviderClient {

    private static final String RESPONSE_TYPE = "code";
    private static final String GRANT_TYPE = "authorization_code";

    private final RestClient restClient;
    private final SpotifyOAuthProperties spotifyOAuthProperties;

    @Override
    public ExternalServiceProvider supports() {
        return ExternalServiceProvider.SPOTIFY;
    }

    @Override
    public String buildAuthorizationUrl(String state) {
        String scope = String.join(
                " ",
                spotifyOAuthProperties.scopes()
        );

        return UriComponentsBuilder
                .fromUriString(
                        spotifyOAuthProperties.authorizationUri()
                )
                .queryParam(
                        "client_id",
                        spotifyOAuthProperties.clientId()
                )
                .queryParam(
                        "response_type",
                        RESPONSE_TYPE
                )
                .queryParam(
                        "redirect_uri",
                        spotifyOAuthProperties.redirectUri()
                )
                .queryParam(
                        "scope",
                        scope
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
        requestBody.add("grant_type", GRANT_TYPE);
        requestBody.add("code", code);
        requestBody.add(
                "redirect_uri",
                spotifyOAuthProperties.redirectUri()
        );

        try {
            SpotifyOAuthTokenResponse response = restClient.post()
                    .uri(spotifyOAuthProperties.tokenUri())
                    .headers(headers -> headers.setBasicAuth(
                            spotifyOAuthProperties.clientId(),
                            spotifyOAuthProperties.clientSecret()
                    ))
                    .contentType(
                            MediaType.APPLICATION_FORM_URLENCODED
                    )
                    .body(requestBody)
                    .retrieve()
                    .body(SpotifyOAuthTokenResponse.class);

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
            SpotifyUserInfoResponse response = restClient.get()
                    .uri(spotifyOAuthProperties.userInfoUri())
                    .headers(headers ->
                            headers.setBearerAuth(accessToken)
                    )
                    .retrieve()
                    .body(SpotifyUserInfoResponse.class);

            if (response == null
                    || response.accountId() == null
                    || response.accountId().isBlank()) {
                throw new BaseException(
                        ErrorCode.OAUTH_USER_INFO_FAILED
                );
            }

            return new ExternalUserInfo(
                    response.accountId(),
                    resolveAccountIdentifier(response)
            );
        } catch (BaseException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BaseException(
                    ErrorCode.OAUTH_USER_INFO_FAILED
            );
        }
    }

    private String resolveAccountIdentifier(
            SpotifyUserInfoResponse response
    ) {
        if (response.email() != null
                && !response.email().isBlank()) {
            return response.email();
        }

        if (response.displayName() != null
                && !response.displayName().isBlank()) {
            return response.displayName();
        }

        return response.id();
    }
}
