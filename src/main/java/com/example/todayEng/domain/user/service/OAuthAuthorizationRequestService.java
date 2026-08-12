package com.example.todayEng.domain.user.service;

import com.example.todayEng.domain.user.entity.OAuthAuthorizationRequest;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.entity.enums.OAuthAuthorizationRequestStatus;
import com.example.todayEng.domain.user.entity.enums.OAuthCallbackFailureStage;
import com.example.todayEng.domain.user.entity.enums.OAuthCallbackFailureType;
import com.example.todayEng.domain.user.repository.OAuthAuthorizationRequestRepository;
import com.example.todayEng.domain.user.repository.UserRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class OAuthAuthorizationRequestService {

    private static final int STATE_BYTE_LENGTH = 32;
    private static final long STATE_VALIDITY_MINUTES = 10L;

    private final OAuthAuthorizationRequestRepository
            oauthAuthorizationRequestRepository;

    private final UserRepository userRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String issue(
            Long userId,
            ExternalServiceProvider provider
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.USER_NOT_FOUND
                ));

        String rawState = generateRawState();
        String stateHash = hashState(rawState);

        OAuthAuthorizationRequest authorizationRequest =
                OAuthAuthorizationRequest.create(
                        user,
                        provider,
                        stateHash,
                        LocalDateTime.now()
                                .plusMinutes(STATE_VALIDITY_MINUTES)
                );

        oauthAuthorizationRequestRepository.save(
                authorizationRequest
        );

        return rawState;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessingClaim startProcessing(
            String rawState,
            ExternalServiceProvider provider
    ) {
        LocalDateTime now = LocalDateTime.now();
        String stateHash = hashState(rawState);

        OAuthAuthorizationRequest authorizationRequest =
                oauthAuthorizationRequestRepository
                        .findByStateHashAndProvider(
                                stateHash,
                                provider
                        )
                        .orElseThrow(() -> new BaseException(
                                ErrorCode.OAUTH_STATE_INVALID
                        ));

        validateState(
                authorizationRequest,
                now
        );

        int updatedCount =
                oauthAuthorizationRequestRepository
                        .startProcessingIfIssued(
                                authorizationRequest.getId(),
                                now,
                                OAuthAuthorizationRequestStatus.ISSUED,
                                OAuthAuthorizationRequestStatus.PROCESSING
                        );

        if (updatedCount == 0) {
            throwStateConsumptionFailure(
                    stateHash,
                    provider,
                    now
            );
        }

        return new ProcessingClaim(
                authorizationRequest.getId(),
                authorizationRequest.getUser().getId()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void succeed(Long requestId) {
        int updated = oauthAuthorizationRequestRepository.markSucceeded(
                requestId,
                OAuthAuthorizationRequestStatus.PROCESSING,
                OAuthAuthorizationRequestStatus.SUCCEEDED,
                LocalDateTime.now()
        );
        if (updated == 0) {
            throw new BaseException(ErrorCode.OAUTH_STATE_CONSUME_FAILED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long requestId, OAuthCallbackFailureStage stage,
                     OAuthCallbackFailureType failureType) {
        oauthAuthorizationRequestRepository.markFailed(
                requestId,
                OAuthAuthorizationRequestStatus.PROCESSING,
                OAuthAuthorizationRequestStatus.FAILED,
                stage,
                failureType,
                LocalDateTime.now()
        );
    }

    private void validateState(
            OAuthAuthorizationRequest authorizationRequest,
            LocalDateTime now
    ) {
        if (authorizationRequest.isExpired(now)) {
            throw new BaseException(
                    ErrorCode.OAUTH_STATE_EXPIRED
            );
        }

        if (authorizationRequest.getStatus() != OAuthAuthorizationRequestStatus.ISSUED) {
            throw new BaseException(
                    ErrorCode.OAUTH_STATE_ALREADY_USED
            );
        }
    }

    private void throwStateConsumptionFailure(
            String stateHash,
            ExternalServiceProvider provider,
            LocalDateTime now
    ) {
        OAuthAuthorizationRequest currentRequest =
                oauthAuthorizationRequestRepository
                        .findByStateHashAndProvider(
                                stateHash,
                                provider
                        )
                        .orElseThrow(() -> new BaseException(
                                ErrorCode.OAUTH_STATE_INVALID
                        ));

        if (currentRequest.isExpired(now)) {
            throw new BaseException(
                    ErrorCode.OAUTH_STATE_EXPIRED
            );
        }

        if (currentRequest.getStatus() != OAuthAuthorizationRequestStatus.ISSUED) {
            throw new BaseException(
                    ErrorCode.OAUTH_STATE_ALREADY_USED
            );
        }

        throw new BaseException(
                ErrorCode.OAUTH_STATE_CONSUME_FAILED
        );
    }

    public record ProcessingClaim(Long requestId, Long userId) {
    }

    private String generateRawState() {
        byte[] randomBytes = new byte[STATE_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hashState(String rawState) {
        if (rawState == null || rawState.isBlank()) {
            throw new BaseException(
                    ErrorCode.OAUTH_STATE_INVALID
            );
        }

        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = messageDigest.digest(
                    rawState.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 알고리즘을 사용할 수 없습니다.",
                    exception
            );
        }
    }
}
