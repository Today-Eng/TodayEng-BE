package com.example.todayEng.domain.notification.service;

import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.notification.dto.WebPushTarget;
import com.example.todayEng.domain.notification.repository.NotificationSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushSubscriptionCleanupServiceTest {

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @InjectMocks
    private PushSubscriptionCleanupService pushSubscriptionCleanupService;

    @Test
    void clearExpiredSubscription_clearsOnlyMatchingSubscription() {
        WebPushTarget target =
                new WebPushTarget(
                        1L,
                        10L,
                        "https://example.com/push",
                        "p256dh-key",
                        "auth-key"
                );

        pushSubscriptionCleanupService.clearExpiredSubscription(
                target
        );

        verify(notificationSettingRepository)
                .clearPushSubscriptionIfMatches(
                        target.notificationSettingId(),
                        target.pushEndpoint(),
                        target.p256dhKey(),
                        target.authKey()
                );
    }
}
