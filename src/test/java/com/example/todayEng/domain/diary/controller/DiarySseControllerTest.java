package com.example.todayEng.domain.diary.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todayEng.domain.diary.service.DiarySubscriptionService;
import com.example.todayEng.global.config.CorsConfig;
import com.example.todayEng.global.config.OAuthSecurityProperties;
import com.example.todayEng.global.config.SecurityConfig;
import com.example.todayEng.global.security.JwtAuthenticationFilter;
import com.example.todayEng.global.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest(DiarySseController.class)
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
class DiarySseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DiarySubscriptionService subscriptionService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void unauthenticatedUserCannotSubscribe() throws Exception {
        mockMvc.perform(get("/diaries/10/subscribe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanSubscribe() throws Exception {
        SseEmitter emitter = new SseEmitter(30_000L);
        given(jwtTokenProvider.parse("valid-token", "access"))
                .willReturn(Jwts.claims().subject("1").build());
        given(subscriptionService.subscribe(1L, 10L))
                .willReturn(emitter);

        mockMvc.perform(get("/diaries/10/subscribe")
                        .header(
                                "Authorization",
                                "Bearer valid-token"
                        ))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(subscriptionService).subscribe(1L, 10L);
        emitter.complete();
    }

    @Test
    void invalidJwtCannotSubscribe() throws Exception {
        mockMvc.perform(get("/diaries/10/subscribe")
                        .header("Authorization", "invalid"))
                .andExpect(status().isUnauthorized());
    }
}
