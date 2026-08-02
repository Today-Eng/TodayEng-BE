package com.example.todayEng.domain.auth.service;

import com.example.todayEng.domain.auth.dto.LoginResponse;
import com.example.todayEng.domain.user.entity.RefreshToken;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.RefreshTokenRepository;
import com.example.todayEng.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponse issueTokens(User user, boolean isNewUser) {
        JwtTokenProvider.IssuedToken access = jwtTokenProvider.issueAccessToken(user.getId());
        JwtTokenProvider.IssuedToken refresh = jwtTokenProvider.issueRefreshToken(user.getId());
        refreshTokenRepository.save(RefreshToken.create(user, refresh.jti(), refresh.expiresAt()));
        return new LoginResponse(access.value(), refresh.value(), isNewUser);
    }
}
