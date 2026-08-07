package com.example.todayEng.domain.auth.service;

import com.example.todayEng.domain.auth.dto.LoginResponse;
import com.example.todayEng.domain.auth.dto.TokenRefreshResponse;
import com.example.todayEng.domain.auth.exception.RefreshTokenReuseException;
import com.example.todayEng.domain.user.entity.*;
import com.example.todayEng.domain.user.entity.enums.AuthProvider;
import com.example.todayEng.domain.user.repository.*;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.example.todayEng.global.security.GoogleTokenVerifier;
import com.example.todayEng.global.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthAccountRepository authAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthAccountProvisioningService provisioningService;
    private final AuthTokenService authTokenService;

    public LoginResponse googleLogin(String idToken) {
        GoogleTokenVerifier.GoogleUser google = googleTokenVerifier.verify(idToken);
        ProvisionedAccount provisioned = getOrCreate(
                AuthProvider.GOOGLE, google.subject(), google.email());
        return authTokenService.issueTokens(
                provisioned.account().getUser(), provisioned.created());
    }

    public LoginResponse testLogin(String socialUid) {
        ProvisionedAccount provisioned = getOrCreate(AuthProvider.TEST, socialUid, null);
        return authTokenService.issueTokens(
                provisioned.account().getUser(), provisioned.created());
    }

    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public TokenRefreshResponse refresh(String token) {
        Claims claims = jwtTokenProvider.parse(token, "refresh");
        String jti = claims.getId();
        String subject = claims.getSubject();
        String sessionId = claims.get("sid", String.class);
        validateRefreshClaims(jti, subject, sessionId);

        RefreshToken storedToken = refreshTokenRepository.findBySessionIdForUpdate(sessionId)
                .orElseThrow(() -> new BaseException(ErrorCode.INVALID_TOKEN));
        if (!storedToken.getUser().getId().toString().equals(subject)) {
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }
        if (!storedToken.getJti().equals(jti)) {
            refreshTokenRepository.delete(storedToken);
            throw new RefreshTokenReuseException();
        }
        if (storedToken.isExpired()) {
            throw new BaseException(ErrorCode.EXPIRED_TOKEN);
        }

        Long userId = storedToken.getUser().getId();
        JwtTokenProvider.IssuedToken newAccessToken = jwtTokenProvider.issueAccessToken(userId);
        JwtTokenProvider.IssuedToken newRefreshToken =
                jwtTokenProvider.issueRefreshToken(userId, sessionId);

        storedToken.rotate(newRefreshToken.jti(), newRefreshToken.expiresAt());

        return new TokenRefreshResponse(newAccessToken.value(), newRefreshToken.value());
    }

    @Transactional
    public void logout(Long authenticatedUserId, String token) {
        Claims claims = jwtTokenProvider.parse(token, "refresh");
        String jti = claims.getId();
        String subject = claims.getSubject();
        String sessionId = claims.get("sid", String.class);
        validateRefreshClaims(jti, subject, sessionId);
        if (!authenticatedUserId.toString().equals(subject)) {
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }

        refreshTokenRepository.findBySessionIdForUpdate(sessionId).ifPresent(storedToken -> {
            if (!storedToken.getUser().getId().equals(authenticatedUserId)) {
                throw new BaseException(ErrorCode.INVALID_TOKEN);
            }
            refreshTokenRepository.delete(storedToken);
        });
    }

    private void validateRefreshClaims(String jti, String subject, String sessionId) {
        if (jti == null || jti.isBlank()
                || subject == null || subject.isBlank()
                || sessionId == null || sessionId.isBlank()) {
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }
    }

    private ProvisionedAccount getOrCreate(
            AuthProvider provider, String providerSubject, String email) {
        return authAccountRepository.findByProviderAndProviderSubject(provider, providerSubject)
                .map(account -> new ProvisionedAccount(account, false))
                .orElseGet(() -> createOrRecover(provider, providerSubject, email));
    }

    private ProvisionedAccount createOrRecover(
            AuthProvider provider, String providerSubject, String email) {
        try {
            return new ProvisionedAccount(
                    provisioningService.create(provider, providerSubject, email), true);
        } catch (DataIntegrityViolationException e) {
            AuthAccount account = authAccountRepository
                    .findByProviderAndProviderSubject(provider, providerSubject)
                    .orElseThrow(() -> e);
            return new ProvisionedAccount(account, false);
        }
    }

    private record ProvisionedAccount(AuthAccount account, boolean created) {}
}
