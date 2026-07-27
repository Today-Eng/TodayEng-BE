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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final AuthAccountRepository authAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponse googleLogin(String idToken) {
        GoogleTokenVerifier.GoogleUser google = googleTokenVerifier.verify(idToken);
        AuthAccount account = authAccountRepository
                .findByProviderAndProviderSubject(AuthProvider.GOOGLE, google.subject())
                .orElse(null);
        boolean isNewUser = account == null;
        User user;
        if (account == null) {
            user = userRepository.save(User.create());
            authAccountRepository.save(AuthAccount.google(user, google.subject(), google.email()));
        } else {
            user = account.getUser();
        }

        return issueTokens(user, isNewUser);
    }

    @Transactional
    public LoginResponse testLogin(String socialUid) {
        AuthAccount account = authAccountRepository
                .findByProviderAndProviderSubject(AuthProvider.TEST, socialUid)
                .orElse(null);
        boolean isNewUser = account == null;
        User user;
        if (account == null) {
            user = userRepository.save(User.create());
            authAccountRepository.save(AuthAccount.test(user, socialUid));
        } else {
            user = account.getUser();
        }
        return issueTokens(user, isNewUser);
    }

    @Transactional
    public void logout(Long authenticatedUserId, String token) {
        Claims claims = jwtTokenProvider.parse(token, "refresh");
        if (!authenticatedUserId.toString().equals(claims.getSubject())) {
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }
        refreshTokenRepository.deleteByJti(claims.getId());
    }

    private LoginResponse issueTokens(User user, boolean isNewUser) {
        JwtTokenProvider.IssuedToken access = jwtTokenProvider.issueAccessToken(user.getId());
        JwtTokenProvider.IssuedToken refresh = jwtTokenProvider.issueRefreshToken(user.getId());
        refreshTokenRepository.save(RefreshToken.create(user, refresh.jti(), refresh.expiresAt()));
        return new LoginResponse(access.value(), refresh.value(), isNewUser);
    }
}
