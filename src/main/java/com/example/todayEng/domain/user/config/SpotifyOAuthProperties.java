package com.example.todayEng.domain.user.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.spotify")
public record SpotifyOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String authorizationUri,
        String tokenUri,
        String userInfoUri,
        List<String> scopes
) {
}
