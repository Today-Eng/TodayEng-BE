package com.example.todayEng.global.config;

import com.example.todayEng.global.common.ApiResponse;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final OAuthSecurityProperties oauthSecurityProperties;

    public SecurityConfig(
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper,
            OAuthSecurityProperties oauthSecurityProperties
    ) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.oauthSecurityProperties = oauthSecurityProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(
                                    ErrorCode.UNAUTHORIZED.getStatus().value()
                            );
                            response.setContentType(
                                    MediaType.APPLICATION_JSON_VALUE
                            );
                            response.setCharacterEncoding("UTF-8");

                            objectMapper.writeValue(
                                    response.getWriter(),
                                    ApiResponse.error(ErrorCode.UNAUTHORIZED)
                            );
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            response.setStatus(
                                    ErrorCode.ACCESS_DENIED.getStatus().value()
                            );
                            response.setContentType(
                                    MediaType.APPLICATION_JSON_VALUE
                            );
                            response.setCharacterEncoding("UTF-8");

                            objectMapper.writeValue(
                                    response.getWriter(),
                                    ApiResponse.error(ErrorCode.ACCESS_DENIED)
                            );
                        })
                )
                .authorizeHttpRequests(authorize -> {
                    authorize.dispatcherTypeMatchers(
                            DispatcherType.ASYNC,
                            DispatcherType.ERROR
                    ).permitAll();

                    authorize.requestMatchers(
                            "/health",
                            "/files/audio/**",
                            "/api/auth/google",
                            "/api/auth/test",
                            "/api/auth/refresh",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/api/external-accounts/*/callback"
                    ).permitAll();

                    if (oauthSecurityProperties.authorizationEndpointPermitAll()) {
                        authorize.requestMatchers(
                                "/api/external-accounts/*/authorization"
                        ).permitAll();
                    }

                    authorize.anyRequest().authenticated();
                })
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
