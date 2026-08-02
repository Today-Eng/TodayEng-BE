package com.example.todayEng.domain.user.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.todayEng.domain.user.config.SpotifyOAuthProperties;
import com.example.todayEng.domain.user.dto.oauth.ExternalUserInfo;
import com.example.todayEng.domain.user.dto.oauth.OAuthTokenResponse;
import com.example.todayEng.domain.user.dto.spotify.SpotifyOAuthTokenResponse;
import com.example.todayEng.domain.user.dto.spotify.SpotifyUserInfoResponse;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpotifyOAuthClient implements OAuthProviderClient {

    private static final String RESPONSE_TYPE = "code";
    private static final String GRANT_TYPE = "authorization_code";
    private static final int MAX_ERROR_DESCRIPTION_LENGTH = 500;

    private final RestClient restClient;
    private final SpotifyOAuthProperties spotifyOAuthProperties;
    private final ObjectMapper objectMapper;

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
        } catch (RestClientResponseException exception) {
            logTokenExchangeFailure(exception);
            throw new BaseException(
                    ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED
            );
        } catch (RestClientException exception) {
            log.warn(
                    "Spotify token exchange request failed: exception={}",
                    exception.getClass().getSimpleName()
            );
            throw new BaseException(
                    ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED
            );
        }
    }

    private void logTokenExchangeFailure(
            RestClientResponseException exception
    ) {
        String error = "unknown";
        String errorDescription = "unavailable";

        try {
            JsonNode responseBody = objectMapper.readTree(
                    exception.getResponseBodyAsString()
            );
            error = sanitizeLogValue(
                    responseBody.path("error").asText("unknown")
            );
            errorDescription = sanitizeLogValue(
                    responseBody.path("error_description")
                            .asText("unavailable")
            );
        } catch (Exception parsingException) {
            log.debug(
                    "Spotify token error response parsing failed: exception={}",
                    parsingException.getClass().getSimpleName()
            );
        }

        log.warn(
                "Spotify token exchange failed: status={}, error={}, description={}",
                exception.getStatusCode().value(),
                error,
                errorDescription
        );
    }

    private String sanitizeLogValue(String value) {
        String sanitized = value
                .replaceAll("[\\r\\n\\t]", " ")
                .trim();

        if (sanitized.length() <= MAX_ERROR_DESCRIPTION_LENGTH) {
            return sanitized;
        }

        return sanitized.substring(
                0,
                MAX_ERROR_DESCRIPTION_LENGTH
        );
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
