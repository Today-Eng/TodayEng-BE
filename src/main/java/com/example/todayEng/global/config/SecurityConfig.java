package com.example.todayEng.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final OAuthSecurityProperties oauthSecurityProperties;

    public SecurityConfig(
            CorsConfigurationSource corsConfigurationSource,
            OAuthSecurityProperties oauthSecurityProperties
    ) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.oauthSecurityProperties = oauthSecurityProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(
                                "/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/external-accounts/*/callback"
                        ).permitAll();

                    if (oauthSecurityProperties
                            .authorizationEndpointPermitAll()) {
                        authorize.requestMatchers(
                                "/api/external-accounts/*/authorization"
                        ).permitAll();
                    }

                    authorize.anyRequest().authenticated();
                });

        return http.build();
    }
}
