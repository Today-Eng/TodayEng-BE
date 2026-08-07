package com.example.todayEng.domain.user.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todayEng.domain.user.dto.response.ExternalAccountListResponse;
import com.example.todayEng.domain.user.dto.response.ExternalAccountResponse;
import com.example.todayEng.domain.user.dto.response.ExternalAccountSettingsResponse;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.service.ExternalAccountService;
import com.example.todayEng.global.config.CorsConfig;
import com.example.todayEng.global.config.OAuthSecurityProperties;
import com.example.todayEng.global.config.SecurityConfig;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.example.todayEng.global.security.JwtAuthenticationFilter;
import com.example.todayEng.global.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * docs/ 의 외부 연동 명세와 실제 URL, 응답 JSON 형태가 어긋나지 않도록 고정하는 테스트.
 * 보안 필터 체인을 그대로 태워서 Bearer 토큰으로 userId가 주입되는 경로까지 검증한다.
 */
@WebMvcTest({
        ExternalAccountController.class,
        ExternalAccountStatusController.class
})
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
class ExternalAccountApiTest {

    private static final Long USER_ID = 1L;
    private static final String BEARER_TOKEN = "Bearer valid-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalAccountService externalAccountService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private Claims claims;

    @BeforeEach
    void authenticateUser() {
        given(jwtTokenProvider.parse("valid-token", "access"))
                .willReturn(claims);
        given(claims.getSubject()).willReturn(String.valueOf(USER_ID));
    }

    @Test
    void 연동_상태_조회는_명세의_URL과_응답_구조를_따른다() throws Exception {
        given(externalAccountService.getExternalAccounts(USER_ID))
                .willReturn(new ExternalAccountListResponse(List.of(
                        new ExternalAccountResponse(
                                1L,
                                ExternalServiceProvider.GOOGLE_CALENDAR,
                                true,
                                true,
                                "example@gmail.com",
                                LocalDateTime.of(2026, 7, 24, 14, 30)
                        ),
                        ExternalAccountResponse.disconnected(
                                ExternalServiceProvider.SPOTIFY
                        )
                )));

        mockMvc.perform(get("/api/integrations/external-accounts")
                        .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.externalAccounts.length()").value(2))
                .andExpect(jsonPath("$.data.externalAccounts[0].externalAccountId").value(1))
                .andExpect(jsonPath("$.data.externalAccounts[0].provider").value("GOOGLE_CALENDAR"))
                .andExpect(jsonPath("$.data.externalAccounts[0].connected").value(true))
                .andExpect(jsonPath("$.data.externalAccounts[0].useEnabled").value(true))
                .andExpect(jsonPath("$.data.externalAccounts[0].accountIdentifier").value("example@gmail.com"))
                .andExpect(jsonPath("$.data.externalAccounts[0].connectedAt").value("2026-07-24T14:30:00"))
                .andExpect(jsonPath("$.data.externalAccounts[1].externalAccountId").doesNotExist())
                .andExpect(jsonPath("$.data.externalAccounts[1].provider").value("SPOTIFY"))
                .andExpect(jsonPath("$.data.externalAccounts[1].connected").value(false))
                .andExpect(jsonPath("$.data.externalAccounts[1].useEnabled").value(false));
    }

    @Test
    void 연동_상태_조회는_인증이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/integrations/external-accounts"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(externalAccountService);
    }

    @Test
    void 사용_설정_변경은_명세의_URL과_응답_구조를_따른다() throws Exception {
        given(externalAccountService.updateUseEnabled(
                USER_ID,
                ExternalServiceProvider.SPOTIFY,
                false
        )).willReturn(new ExternalAccountSettingsResponse(
                ExternalServiceProvider.SPOTIFY,
                true,
                false
        ));

        mockMvc.perform(patch("/api/external-accounts/SPOTIFY/settings")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"useEnabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("외부 서비스 연동 설정이 변경되었습니다."))
                .andExpect(jsonPath("$.data.provider").value("SPOTIFY"))
                .andExpect(jsonPath("$.data.connected").value(true))
                .andExpect(jsonPath("$.data.useEnabled").value(false));
    }

    @Test
    void 사용_설정_변경은_slug_형태의_provider도_받는다() throws Exception {
        given(externalAccountService.updateUseEnabled(
                USER_ID,
                ExternalServiceProvider.GOOGLE_CALENDAR,
                true
        )).willReturn(new ExternalAccountSettingsResponse(
                ExternalServiceProvider.GOOGLE_CALENDAR,
                true,
                true
        ));

        mockMvc.perform(patch("/api/external-accounts/google-calendar/settings")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"useEnabled\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("GOOGLE_CALENDAR"));
    }

    @Test
    void 사용_설정_변경은_지원하지_않는_provider면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/external-accounts/unknown/settings")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"useEnabled\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(externalAccountService);
    }

    @Test
    void 사용_설정_변경은_useEnabled가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/external-accounts/SPOTIFY/settings")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 사용_설정_변경은_연동되지_않은_provider면_404를_반환한다() throws Exception {
        given(externalAccountService.updateUseEnabled(
                USER_ID,
                ExternalServiceProvider.SPOTIFY,
                false
        )).willThrow(new BaseException(
                ErrorCode.EXTERNAL_ACCOUNT_NOT_CONNECTED
        ));

        mockMvc.perform(patch("/api/external-accounts/SPOTIFY/settings")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"useEnabled\": false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("연동되지 않은 외부 서비스입니다."));
    }

    @Test
    void 연동_해제는_명세의_URL과_응답_구조를_따른다() throws Exception {
        mockMvc.perform(delete("/api/external-accounts/SPOTIFY")
                        .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("외부 서비스 연동이 해제되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(externalAccountService).disconnect(
                USER_ID,
                ExternalServiceProvider.SPOTIFY
        );
    }

    @Test
    void 연동_해제는_지원하지_않는_provider면_400을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/external-accounts/unknown")
                        .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
