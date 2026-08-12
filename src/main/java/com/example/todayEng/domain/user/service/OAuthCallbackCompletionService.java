package com.example.todayEng.domain.user.service;

import com.example.todayEng.domain.user.dto.oauth.ExternalUserInfo;
import com.example.todayEng.domain.user.dto.oauth.OAuthTokenResponse;
import com.example.todayEng.domain.user.entity.OAuthAuthorizationRequest;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.OAuthAuthorizationRequestRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthCallbackCompletionService {

    private final OAuthAuthorizationRequestRepository authorizationRequestRepository;
    private final ExternalAccountConnectionService externalAccountConnectionService;

    @Transactional
    public void saveAccountAndSucceed(
            Long requestId,
            Long userId,
            ExternalServiceProvider provider,
            OAuthTokenResponse tokenResponse,
            ExternalUserInfo externalUserInfo
    ) {
        OAuthAuthorizationRequest request = authorizationRequestRepository
                .findByIdForUpdate(requestId)
                .orElseThrow(() -> new BaseException(ErrorCode.OAUTH_STATE_INVALID));
        try {
            request.succeed(LocalDateTime.now());
        } catch (IllegalStateException exception) {
            throw new BaseException(ErrorCode.OAUTH_STATE_CONSUME_FAILED);
        }
        externalAccountConnectionService.saveOrUpdate(
                userId, provider, tokenResponse, externalUserInfo);
    }
}
