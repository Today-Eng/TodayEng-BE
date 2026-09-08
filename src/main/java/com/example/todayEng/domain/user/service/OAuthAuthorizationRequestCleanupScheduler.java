package com.example.todayEng.domain.user.service;

import com.example.todayEng.domain.user.entity.enums.OAuthAuthorizationRequestStatus;
import com.example.todayEng.domain.user.repository.OAuthAuthorizationRequestRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthAuthorizationRequestCleanupScheduler {

    private static final Duration RETENTION = Duration.ofDays(7);
    private static final Set<OAuthAuthorizationRequestStatus> DELETABLE_STATUSES = Set.of(
            OAuthAuthorizationRequestStatus.LEGACY,
            OAuthAuthorizationRequestStatus.ISSUED,
            OAuthAuthorizationRequestStatus.SUCCEEDED,
            OAuthAuthorizationRequestStatus.FAILED
    );

    private final OAuthAuthorizationRequestRepository repository;
    private final Clock clock;

    @Scheduled(
            cron = "${oauth.callback.cleanup-cron:0 0 3 * * *}",
            zone = "${oauth.callback.cleanup-zone:Asia/Seoul}"
    )
    @SchedulerLock(
            name = "oauthAuthorizationRequestCleanupScheduler",
            lockAtLeastFor = "1m",
            lockAtMostFor = "30m"
    )
    @Transactional
    public void deleteExpiredRequests() {
        LocalDateTime threshold = LocalDateTime.now(clock).minus(RETENTION);
        int deleted = repository.deleteExpiredRequests(
                threshold,
                DELETABLE_STATUSES
        );

        if (deleted > 0) {
            log.info("Expired OAuth authorization requests deleted: count={}", deleted);
        }
    }
}
