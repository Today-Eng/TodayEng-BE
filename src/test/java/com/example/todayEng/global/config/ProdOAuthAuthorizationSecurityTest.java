package com.example.todayEng.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todayEng.domain.user.controller.ExternalAccountOAuthController;
import com.example.todayEng.domain.user.service.ExternalAccountOAuthService;
import com.example.todayEng.global.security.JwtAuthenticationFilter;
import com.example.todayEng.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExternalAccountOAuthController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        JwtAuthenticationFilter.class
})
@EnableConfigurationProperties(OAuthSecurityProperties.class)
@TestPropertySource(properties = {
        "security.oauth.authorization-endpoint-permit-all=false",
        "cors.allowed-origins=http://localhost"
})
class ProdOAuthAuthorizationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalAccountOAuthService externalAccountOAuthService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void authorization_withoutAuthentication_isForbidden()
            throws Exception {
        mockMvc.perform(post(
                        "/api/external-accounts/spotify/authorization"
                )
                        .param("userId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void callback_withoutAuthentication_isAllowed()
            throws Exception {
        mockMvc.perform(get(
                        "/api/external-accounts/spotify/callback"
                )
                        .param("code", "authorization-code")
                        .param("state", "state-value"))
                .andExpect(status().isOk());
    }
}
