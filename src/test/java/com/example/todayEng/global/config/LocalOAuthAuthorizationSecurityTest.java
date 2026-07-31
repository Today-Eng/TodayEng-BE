package com.example.todayEng.global.config;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todayEng.domain.user.controller.ExternalAccountOAuthController;
import com.example.todayEng.domain.user.dto.response.OAuthAuthorizationResponse;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.service.ExternalAccountOAuthService;
import com.example.todayEng.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExternalAccountOAuthController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@EnableConfigurationProperties(OAuthSecurityProperties.class)
@TestPropertySource(properties = {
        "security.oauth.authorization-endpoint-permit-all=true",
        "cors.allowed-origins=http://localhost"
})
class LocalOAuthAuthorizationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalAccountOAuthService externalAccountOAuthService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void authorization_withoutAuthentication_isAllowed() throws Exception {
        given(externalAccountOAuthService.createAuthorizationUrl(
                1L,
                ExternalServiceProvider.SPOTIFY
        )).willReturn(new OAuthAuthorizationResponse(
                "https://accounts.spotify.com/authorize"
        ));

        mockMvc.perform(post(
                        "/api/external-accounts/spotify/authorization"
                )
                        .param("userId", "1"))
                .andExpect(status().isOk());
    }
}
