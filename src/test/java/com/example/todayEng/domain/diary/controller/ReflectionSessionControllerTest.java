package com.example.todayEng.domain.diary.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse;
import com.example.todayEng.domain.diary.service.ReflectionSessionService;
import com.example.todayEng.global.config.CorsConfig;
import com.example.todayEng.global.config.OAuthSecurityProperties;
import com.example.todayEng.global.config.SecurityConfig;
import com.example.todayEng.global.security.JwtAuthenticationFilter;
import com.example.todayEng.global.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReflectionSessionController.class)
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
class ReflectionSessionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ReflectionSessionService reflectionSessionService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    void authenticatedOwnerCanCreateReflectionSession() throws Exception {
        given(jwtTokenProvider.parse("valid-token", "access"))
                .willReturn(Jwts.claims().subject("1").build());
        given(reflectionSessionService.create(1L, 10L))
                .willReturn(new ReflectionSessionResponse(10L, List.of()));

        mockMvc.perform(post("/api/diaries/10/reflection-sessions")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.diaryId").value(10));

        verify(reflectionSessionService).create(1L, 10L);
    }

    @Test
    void unauthenticatedUserCannotCreateReflectionSession() throws Exception {
        mockMvc.perform(post("/api/diaries/10/reflection-sessions"))
                .andExpect(status().isUnauthorized());
    }
}
