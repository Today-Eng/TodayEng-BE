package com.example.todayEng.domain.auth.service;

import com.example.todayEng.domain.auth.dto.LoginResponse;
import com.example.todayEng.domain.user.entity.RefreshToken;
import com.example.todayEng.domain.user.entity.AuthAccount;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.enums.AuthProvider;
import com.example.todayEng.domain.user.repository.*;
import com.example.todayEng.global.security.GoogleTokenVerifier;
import com.example.todayEng.global.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AuthAccountRepository authAccountRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock GoogleTokenVerifier googleTokenVerifier;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock AuthAccountProvisioningService provisioningService;
    @InjectMocks AuthService authService;

    @Test
    void googleLoginCreatesNewUserAndRefreshToken() {
        when(googleTokenVerifier.verify("google-token"))
                .thenReturn(new GoogleTokenVerifier.GoogleUser("sub-1", "user@example.com"));
        when(authAccountRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, "sub-1"))
                .thenReturn(Optional.empty());
        User user = User.create("user@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(provisioningService.create(AuthProvider.GOOGLE, "sub-1", "user@example.com"))
                .thenReturn(AuthAccount.google(user, "sub-1"));
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(14);
        when(jwtTokenProvider.issueAccessToken(1L))
                .thenReturn(new JwtTokenProvider.IssuedToken("access", "access-jti", expiresAt));
        when(jwtTokenProvider.issueRefreshToken(1L))
                .thenReturn(new JwtTokenProvider.IssuedToken("refresh", "refresh-jti", expiresAt));

        LoginResponse response = authService.googleLogin("google-token");

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.isNewUser()).isTrue();
        verify(provisioningService).create(AuthProvider.GOOGLE, "sub-1", "user@example.com");
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getJti()).isEqualTo("refresh-jti");
    }

    @Test
    void logoutDeletesRefreshTokenByJti() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(claims.getId()).thenReturn("refresh-jti");
        when(jwtTokenProvider.parse("refresh", "refresh")).thenReturn(claims);

        authService.logout(1L, "refresh");

        verify(refreshTokenRepository).deleteByJti("refresh-jti");
    }

    @Test
    void testLoginCreatesUserFromSocialUid() {
        when(authAccountRepository.findByProviderAndProviderSubject(AuthProvider.TEST, "local-user"))
                .thenReturn(Optional.empty());
        User user = User.create();
        ReflectionTestUtils.setField(user, "id", 7L);
        when(provisioningService.create(AuthProvider.TEST, "local-user", null))
                .thenReturn(AuthAccount.test(user, "local-user"));
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(14);
        when(jwtTokenProvider.issueAccessToken(7L))
                .thenReturn(new JwtTokenProvider.IssuedToken("test-access", "access-jti", expiresAt));
        when(jwtTokenProvider.issueRefreshToken(7L))
                .thenReturn(new JwtTokenProvider.IssuedToken("test-refresh", "refresh-jti", expiresAt));

        LoginResponse response = authService.testLogin("local-user");

        assertThat(response.isNewUser()).isTrue();
        assertThat(response.accessToken()).isEqualTo("test-access");
        verify(provisioningService).create(AuthProvider.TEST, "local-user", null);
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void googleLoginRecoversWhenConcurrentAccountCreationWins() {
        when(googleTokenVerifier.verify("google-token"))
                .thenReturn(new GoogleTokenVerifier.GoogleUser("sub-1", "user@example.com"));
        User winner = User.create("user@example.com");
        ReflectionTestUtils.setField(winner, "id", 11L);
        AuthAccount winnerAccount = AuthAccount.google(winner, "sub-1");
        when(authAccountRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, "sub-1"))
                .thenReturn(Optional.empty(), Optional.of(winnerAccount));
        when(provisioningService.create(AuthProvider.GOOGLE, "sub-1", "user@example.com"))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));
        stubTokens(11L);

        LoginResponse response = authService.googleLogin("google-token");

        assertThat(response.isNewUser()).isFalse();
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void testLoginRecoversWhenConcurrentAccountCreationWins() {
        User winner = User.create();
        ReflectionTestUtils.setField(winner, "id", 12L);
        AuthAccount winnerAccount = AuthAccount.test(winner, "same-uid");
        when(authAccountRepository.findByProviderAndProviderSubject(AuthProvider.TEST, "same-uid"))
                .thenReturn(Optional.empty(), Optional.of(winnerAccount));
        when(provisioningService.create(AuthProvider.TEST, "same-uid", null))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));
        stubTokens(12L);

        LoginResponse response = authService.testLogin("same-uid");

        assertThat(response.isNewUser()).isFalse();
        verify(refreshTokenRepository).save(any());
    }

    private void stubTokens(Long userId) {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(14);
        when(jwtTokenProvider.issueAccessToken(userId))
                .thenReturn(new JwtTokenProvider.IssuedToken("access", "access-jti", expiresAt));
        when(jwtTokenProvider.issueRefreshToken(userId))
                .thenReturn(new JwtTokenProvider.IssuedToken("refresh", "refresh-jti", expiresAt));
    }
}
