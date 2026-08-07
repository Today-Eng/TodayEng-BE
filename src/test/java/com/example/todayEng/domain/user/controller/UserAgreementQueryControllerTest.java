package com.example.todayEng.domain.user.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todayEng.domain.user.dto.UserDtos.AgreementResponse;
import com.example.todayEng.domain.user.dto.UserDtos.AgreementStatus;
import com.example.todayEng.domain.user.dto.UserDtos.AgreementsResponse;
import com.example.todayEng.domain.user.entity.enums.TermsType;
import com.example.todayEng.domain.user.service.UserService;
import com.example.todayEng.global.config.CorsConfig;
import com.example.todayEng.global.config.OAuthSecurityProperties;
import com.example.todayEng.global.config.SecurityConfig;
import com.example.todayEng.global.security.JwtAuthenticationFilter;
import com.example.todayEng.global.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
@EnableConfigurationProperties(OAuthSecurityProperties.class)
@TestPropertySource(properties = {
        "security.oauth.authorization-endpoint-permit-all=false",
        "cors.allowed-origins=http://localhost"
})
class UserAgreementQueryControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean UserService userService;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @Test
    void authenticatedUserCanGetAgreements() throws Exception {
        given(jwtTokenProvider.parse("valid-token", "access"))
                .willReturn(Jwts.claims().subject("1").build());
        given(userService.getAgreements(1L)).willReturn(new AgreementsResponse(
                true,
                List.of(new AgreementResponse(
                        1L,
                        TermsType.SERVICE_USE,
                        "서비스 이용약관 동의",
                        "약관 내용",
                        true,
                        1,
                        AgreementStatus.AGREED,
                        LocalDateTime.of(2026, 8, 7, 10, 30)
                ))
        ));

        mockMvc.perform(get("/api/users/me/agreements")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.allRequiredAgreed").value(true))
                .andExpect(jsonPath("$.data.agreements[0].termId").value(1))
                .andExpect(jsonPath("$.data.agreements[0].termsType").value("SERVICE_USE"))
                .andExpect(jsonPath("$.data.agreements[0].agreementStatus").value("AGREED"));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/users/me/agreements"))
                .andExpect(status().isUnauthorized());
    }
}
