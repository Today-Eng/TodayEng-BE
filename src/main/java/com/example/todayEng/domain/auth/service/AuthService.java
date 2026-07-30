package com.example.todayEng.domain.auth.service;

import com.example.todayEng.domain.auth.dto.LoginResponse;
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

    public LoginResponse googleLogin(String idToken) {
        GoogleTokenVerifier.GoogleUser google = googleTokenVerifier.verify(idToken);
        ProvisionedAccount provisioned = getOrCreate(
                AuthProvider.GOOGLE, google.subject(), google.email());
        return issueTokens(provisioned.account().getUser(), provisioned.created());
    }

    public LoginResponse testLogin(String socialUid) {
        ProvisionedAccount provisioned = getOrCreate(AuthProvider.TEST, socialUid, null);
        return issueTokens(provisioned.account().getUser(), provisioned.created());
    }

    @Transactional
    public void logout(Long authenticatedUserId, String token) {
        Claims claims = jwtTokenProvider.parse(token, "refresh");
        if (!authenticatedUserId.toString().equals(claims.getSubject())) {
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }
        refreshTokenRepository.deleteByJti(claims.getId());
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

    @Transactional
    protected LoginResponse issueTokens(User user, boolean isNewUser) {
        JwtTokenProvider.IssuedToken access = jwtTokenProvider.issueAccessToken(user.getId());
        JwtTokenProvider.IssuedToken refresh = jwtTokenProvider.issueRefreshToken(user.getId());
        refreshTokenRepository.save(RefreshToken.create(user, refresh.jti(), refresh.expiresAt()));
        return new LoginResponse(access.value(), refresh.value(), isNewUser);
    }

    private record ProvisionedAccount(AuthAccount account, boolean created) {}
}
