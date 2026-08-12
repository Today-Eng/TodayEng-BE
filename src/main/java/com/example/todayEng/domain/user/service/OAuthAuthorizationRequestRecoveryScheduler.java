package com.example.todayEng.domain.user.service;

import com.example.todayEng.domain.user.entity.enums.OAuthAuthorizationRequestStatus;
import com.example.todayEng.domain.user.entity.enums.OAuthCallbackFailureStage;
import com.example.todayEng.domain.user.entity.enums.OAuthCallbackFailureType;
import com.example.todayEng.domain.user.repository.OAuthAuthorizationRequestRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthAuthorizationRequestRecoveryScheduler {

    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(15);

    private final OAuthAuthorizationRequestRepository repository;

    @Scheduled(fixedDelayString = "${oauth.callback.recovery-interval-millis:60000}")
    @Transactional
    public void failStaleProcessingRequests() {
        LocalDateTime now = LocalDateTime.now();
        int updated = repository.failStaleProcessing(
                OAuthAuthorizationRequestStatus.PROCESSING,
                OAuthAuthorizationRequestStatus.FAILED,
                OAuthCallbackFailureStage.STALE_PROCESSING,
                OAuthCallbackFailureType.INTERNAL,
                now.minus(PROCESSING_TIMEOUT),
                now
        );
        if (updated > 0) {
            log.warn("Stale OAuth callback requests marked failed: count={}", updated);
        }
    }
}
