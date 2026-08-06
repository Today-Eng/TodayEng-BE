package com.example.todayEng.domain.diary.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todayEng.domain.diary.dto.response.DiaryPauseResponse;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.service.DiaryPauseService;
import com.example.todayEng.global.config.CorsConfig;
import com.example.todayEng.global.config.OAuthSecurityProperties;
import com.example.todayEng.global.config.SecurityConfig;
import com.example.todayEng.global.security.JwtAuthenticationFilter;
import com.example.todayEng.global.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DiaryPauseController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
@EnableConfigurationProperties(OAuthSecurityProperties.class)
@TestPropertySource(properties = {
        "security.oauth.authorization-endpoint-permit-all=false",
        "cors.allowed-origins=http://localhost"
})
class DiaryPauseControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DiaryPauseService diaryPauseService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    void authenticatedUserCanPauseDiary() throws Exception {
        LocalDateTime pausedAt = LocalDateTime.of(2026, 8, 3, 22, 30);
        given(jwtTokenProvider.parse("valid-token", "access"))
                .willReturn(Jwts.claims().subject("1").build());
        given(diaryPauseService.pause(1L, 10L)).willReturn(new DiaryPauseResponse(
                10L, DiaryStatus.PAUSED, pausedAt, pausedAt.plusHours(24)));

        mockMvc.perform(patch("/api/diaries/10/pause")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.diaryId").value(10))
                .andExpect(jsonPath("$.data.status").value("PAUSED"))
                .andExpect(jsonPath("$.data.expiresAt").value("2026-08-04T22:30:00"));

        verify(diaryPauseService).pause(1L, 10L);
    }

    @Test
    void unauthenticatedUserCannotPauseDiary() throws Exception {
        mockMvc.perform(patch("/api/diaries/10/pause"))
                .andExpect(status().isUnauthorized());
    }
}
