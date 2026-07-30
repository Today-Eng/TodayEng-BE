package com.example.todayEng.domain.auth.service;

import com.example.todayEng.domain.auth.dto.LoginResponse;
import com.example.todayEng.domain.user.entity.RefreshToken;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @Mock AuthAccountRepository authAccountRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock GoogleTokenVerifier googleTokenVerifier;
    @Mock JwtTokenProvider jwtTokenProvider;
    @InjectMocks AuthService authService;

    @Test
    void googleLoginCreatesNewUserAndRefreshToken() {
        when(googleTokenVerifier.verify("google-token"))
                .thenReturn(new GoogleTokenVerifier.GoogleUser("sub-1", "user@example.com"));
        when(authAccountRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, "sub-1"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(14);
        when(jwtTokenProvider.issueAccessToken(1L))
                .thenReturn(new JwtTokenProvider.IssuedToken("access", "access-jti", expiresAt));
        when(jwtTokenProvider.issueRefreshToken(1L))
                .thenReturn(new JwtTokenProvider.IssuedToken("refresh", "refresh-jti", expiresAt));

        LoginResponse response = authService.googleLogin("google-token");

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.isNewUser()).isTrue();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        verify(authAccountRepository).save(any());
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
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 7L);
            return user;
        });
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(14);
        when(jwtTokenProvider.issueAccessToken(7L))
                .thenReturn(new JwtTokenProvider.IssuedToken("test-access", "access-jti", expiresAt));
        when(jwtTokenProvider.issueRefreshToken(7L))
                .thenReturn(new JwtTokenProvider.IssuedToken("test-refresh", "refresh-jti", expiresAt));

        LoginResponse response = authService.testLogin("local-user");

        assertThat(response.isNewUser()).isTrue();
        assertThat(response.accessToken()).isEqualTo("test-access");
        verify(authAccountRepository).save(any());
        verify(refreshTokenRepository).save(any());
    }
}
