package com.example.todayEng.domain.diary.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todayEng.domain.diary.dto.response.DiaryMemoUpdateResponse;
import com.example.todayEng.domain.diary.service.DiaryMemoService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DiaryMemoController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
@EnableConfigurationProperties(OAuthSecurityProperties.class)
@TestPropertySource(properties = {
        "security.oauth.authorization-endpoint-permit-all=false",
        "cors.allowed-origins=http://localhost"
})
class DiaryMemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DiaryMemoService diaryMemoService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void authenticatedUserCanUpdateMemo() throws Exception {
        given(jwtTokenProvider.parse("valid-token", "access"))
                .willReturn(Jwts.claims().subject("1").build());
        given(diaryMemoService.updateMemo(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.any()
        )).willReturn(new DiaryMemoUpdateResponse(10L, "수정된 메모"));

        mockMvc.perform(patch("/api/diaries/10/memo")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memo": "수정된 메모"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.diaryId").value(10))
                .andExpect(jsonPath("$.data.memo").value("수정된 메모"));

        verify(diaryMemoService).updateMemo(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void rejectsTooLongMemo() throws Exception {
        given(jwtTokenProvider.parse("valid-token", "access"))
                .willReturn(Jwts.claims().subject("1").build());

        mockMvc.perform(patch("/api/diaries/10/memo")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memo": "%s"
                                }
                                """.formatted("x".repeat(2001))))
                .andExpect(status().isBadRequest());
    }
}
