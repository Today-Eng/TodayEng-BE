package com.example.todayEng.domain.auth.service;

import com.example.todayEng.domain.auth.dto.LoginResponse;
import com.example.todayEng.domain.user.entity.RefreshToken;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.RefreshTokenRepository;
import com.example.todayEng.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtTokenProvider jwtTokenProvider;
    @InjectMocks AuthTokenService authTokenService;

    @Test
    void issueTokensSavesRefreshToken() {
        User user = User.create("user@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(14);
        when(jwtTokenProvider.issueAccessToken(1L))
                .thenReturn(new JwtTokenProvider.IssuedToken(
                        "access", "access-jti", expiresAt));
        when(jwtTokenProvider.issueRefreshToken(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new JwtTokenProvider.IssuedToken(
                        "refresh", "refresh-jti", expiresAt));

        LoginResponse response = authTokenService.issueTokens(user, true);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.isNewUser()).isTrue();
        ArgumentCaptor<RefreshToken> tokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getJti()).isEqualTo("refresh-jti");
        assertThat(tokenCaptor.getValue().getSessionId()).isNotBlank();
    }
}
