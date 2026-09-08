package com.example.todayEng.domain.user.service;

import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.user.entity.enums.OAuthAuthorizationRequestStatus;
import com.example.todayEng.domain.user.repository.OAuthAuthorizationRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthAuthorizationRequestCleanupSchedulerTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock OAuthAuthorizationRequestRepository repository;

    @Test
    void deletesOnlyTerminalOrUnusedRequestsExpiredForSevenDays() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-08T03:00:00Z"), SEOUL);
        OAuthAuthorizationRequestCleanupScheduler scheduler =
                new OAuthAuthorizationRequestCleanupScheduler(repository, clock);

        scheduler.deleteExpiredRequests();

        verify(repository).deleteExpiredRequests(
                LocalDateTime.of(2026, 9, 1, 12, 0),
                Set.of(
                        OAuthAuthorizationRequestStatus.LEGACY,
                        OAuthAuthorizationRequestStatus.ISSUED,
                        OAuthAuthorizationRequestStatus.SUCCEEDED,
                        OAuthAuthorizationRequestStatus.FAILED
                ));
    }
}
