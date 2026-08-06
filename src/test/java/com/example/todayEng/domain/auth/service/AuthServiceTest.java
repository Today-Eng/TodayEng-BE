package com.example.todayEng.domain.auth.service;

import com.example.todayEng.domain.auth.dto.LoginResponse;
import com.example.todayEng.domain.auth.dto.TokenRefreshResponse;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AuthAccountRepository authAccountRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock GoogleTokenVerifier googleTokenVerifier;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock AuthAccountProvisioningService provisioningService;
    @Mock AuthTokenService authTokenService;
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
        when(authTokenService.issueTokens(user, true))
                .thenReturn(new LoginResponse("access", "refresh", true));

        LoginResponse response = authService.googleLogin("google-token");

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.isNewUser()).isTrue();
        verify(provisioningService).create(AuthProvider.GOOGLE, "sub-1", "user@example.com");
        verify(authTokenService).issueTokens(user, true);
    }

    @Test
    void logoutDeletesCurrentSessionEvenWhenPresentedTokenWasAlreadyRotated() {
        User user = User.create("user@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        RefreshToken storedToken = RefreshToken.create(
                user, "session-id", "rotated-jti", LocalDateTime.now().plusDays(1));
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(claims.getId()).thenReturn("refresh-jti");
        when(claims.get("sid", String.class)).thenReturn("session-id");
        when(jwtTokenProvider.parse("refresh", "refresh")).thenReturn(claims);
        when(refreshTokenRepository.findBySessionIdForUpdate("session-id"))
                .thenReturn(Optional.of(storedToken));

        authService.logout(1L, "refresh");

        verify(refreshTokenRepository).delete(storedToken);
    }

    @Test
    void refreshRejectsPreviousJtiFromSameSession() {
        User user = User.create("user@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        RefreshToken storedToken = RefreshToken.create(
                user, "session-id", "current-jti", LocalDateTime.now().plusDays(1));
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(claims.getId()).thenReturn("previous-jti");
        when(claims.get("sid", String.class)).thenReturn("session-id");
        when(jwtTokenProvider.parse("previous-refresh", "refresh")).thenReturn(claims);
        when(refreshTokenRepository.findBySessionIdForUpdate("session-id"))
                .thenReturn(Optional.of(storedToken));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> authService.refresh("previous-refresh"))
                .isInstanceOf(com.example.todayEng.global.error.exception.BaseException.class)
                .extracting("errorCode")
                .isEqualTo(com.example.todayEng.global.error.ErrorCode.INVALID_TOKEN);

        verify(jwtTokenProvider, never()).issueAccessToken(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void refreshRotatesStoredRefreshTokenAndIssuesNewTokenPair() {
        User user = User.create("user@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        RefreshToken storedToken = RefreshToken.create(
                user, "session-id", "old-jti", LocalDateTime.now().plusDays(1));
        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn("old-jti");
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("sid", String.class)).thenReturn("session-id");
        when(jwtTokenProvider.parse("old-refresh", "refresh")).thenReturn(claims);
        when(refreshTokenRepository.findBySessionIdForUpdate("session-id"))
                .thenReturn(Optional.of(storedToken));
        when(jwtTokenProvider.issueAccessToken(1L)).thenReturn(
                new JwtTokenProvider.IssuedToken("new-access", "access-jti", LocalDateTime.now().plusHours(1)));
        when(jwtTokenProvider.issueRefreshToken(1L, "session-id")).thenReturn(
                new JwtTokenProvider.IssuedToken("new-refresh", "new-jti", LocalDateTime.now().plusDays(14)));

        TokenRefreshResponse response = authService.refresh("old-refresh");

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(storedToken.getJti()).isEqualTo("new-jti");
        assertThat(storedToken.getSessionId()).isEqualTo("session-id");
    }

    @Test
    void refreshRejectsTokenThatIsNotStored() {
        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn("revoked-jti");
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("sid", String.class)).thenReturn("session-id");
        when(jwtTokenProvider.parse("revoked-refresh", "refresh")).thenReturn(claims);
        when(refreshTokenRepository.findBySessionIdForUpdate("session-id"))
                .thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> authService.refresh("revoked-refresh"))
                .isInstanceOf(com.example.todayEng.global.error.exception.BaseException.class)
                .extracting("errorCode")
                .isEqualTo(com.example.todayEng.global.error.ErrorCode.INVALID_TOKEN);

        verify(jwtTokenProvider, never()).issueAccessToken(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void refreshRejectsExpiredStoredToken() {
        User user = User.create("user@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        RefreshToken storedToken = RefreshToken.create(
                user, "session-id", "expired-jti", LocalDateTime.now().minusSeconds(1));
        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn("expired-jti");
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("sid", String.class)).thenReturn("session-id");
        when(jwtTokenProvider.parse("expired-refresh", "refresh")).thenReturn(claims);
        when(refreshTokenRepository.findBySessionIdForUpdate("session-id"))
                .thenReturn(Optional.of(storedToken));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> authService.refresh("expired-refresh"))
                .isInstanceOf(com.example.todayEng.global.error.exception.BaseException.class)
                .extracting("errorCode")
                .isEqualTo(com.example.todayEng.global.error.ErrorCode.EXPIRED_TOKEN);

        verify(jwtTokenProvider, never()).issueAccessToken(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void testLoginCreatesUserFromSocialUid() {
        when(authAccountRepository.findByProviderAndProviderSubject(AuthProvider.TEST, "local-user"))
                .thenReturn(Optional.empty());
        User user = User.create();
        ReflectionTestUtils.setField(user, "id", 7L);
        when(provisioningService.create(AuthProvider.TEST, "local-user", null))
                .thenReturn(AuthAccount.test(user, "local-user"));
        when(authTokenService.issueTokens(user, true))
                .thenReturn(new LoginResponse("test-access", "test-refresh", true));

        LoginResponse response = authService.testLogin("local-user");

        assertThat(response.isNewUser()).isTrue();
        assertThat(response.accessToken()).isEqualTo("test-access");
        verify(provisioningService).create(AuthProvider.TEST, "local-user", null);
        verify(authTokenService).issueTokens(user, true);
    }

    @Test
    void googleLoginUsesExistingAccountWhenCreationConflictOccurs() {
        when(googleTokenVerifier.verify("google-token"))
                .thenReturn(new GoogleTokenVerifier.GoogleUser("sub-1", "user@example.com"));
        User winner = User.create("user@example.com");
        ReflectionTestUtils.setField(winner, "id", 11L);
        AuthAccount winnerAccount = AuthAccount.google(winner, "sub-1");
        when(authAccountRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, "sub-1"))
                .thenReturn(Optional.empty(), Optional.of(winnerAccount));
        when(provisioningService.create(AuthProvider.GOOGLE, "sub-1", "user@example.com"))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));
        when(authTokenService.issueTokens(winner, false))
                .thenReturn(new LoginResponse("access", "refresh", false));

        LoginResponse response = authService.googleLogin("google-token");

        assertThat(response.isNewUser()).isFalse();
        verify(authTokenService).issueTokens(winner, false);
    }

    @Test
    void testLoginUsesExistingAccountWhenCreationConflictOccurs() {
        User winner = User.create();
        ReflectionTestUtils.setField(winner, "id", 12L);
        AuthAccount winnerAccount = AuthAccount.test(winner, "same-uid");
        when(authAccountRepository.findByProviderAndProviderSubject(AuthProvider.TEST, "same-uid"))
                .thenReturn(Optional.empty(), Optional.of(winnerAccount));
        when(provisioningService.create(AuthProvider.TEST, "same-uid", null))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));
        when(authTokenService.issueTokens(winner, false))
                .thenReturn(new LoginResponse("access", "refresh", false));

        LoginResponse response = authService.testLogin("same-uid");

        assertThat(response.isNewUser()).isFalse();
        verify(authTokenService).issueTokens(winner, false);
    }
}
