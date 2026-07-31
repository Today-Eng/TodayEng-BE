package com.example.todayEng.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.oauth")
public record OAuthSecurityProperties(
        boolean authorizationEndpointPermitAll
) {
}
