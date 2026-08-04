package com.example.todayEng.domain.notification.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.example.todayEng.domain.notification.dto.WebPushTarget;
import com.example.todayEng.domain.notification.exception.PushSubscriptionExpiredException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationTargetReader notificationTargetReader;

    @Mock
    private WebPushService webPushService;

    @Mock
    private PushSubscriptionCleanupService pushSubscriptionCleanupService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("회고 알림 발송 중 구독이 만료되면 별도 서비스로 구독 정보를 정리한다")
    void sendDiaryReminders_expiredSubscription_cleansUp() {
        LocalDate today =
                LocalDate.of(2026, 8, 2);

        WebPushTarget target =
                new WebPushTarget(
                        10L,
                        1L,
                        "https://example.com/push",
                        "p256dh-key",
                        "auth-key"
                );

        given(
                notificationTargetReader.findDiaryReminderTargets(
                        today
                )
        ).willReturn(List.of(target));

        willThrow(new PushSubscriptionExpiredException())
                .given(webPushService)
                .send(
                        target,
                        "오늘의 회고를 남겨볼까요?",
                        "오늘 하루를 영어로 천천히 돌아보세요.",
                        "/home"
                );

        notificationService.sendDiaryReminders(today);

        verify(pushSubscriptionCleanupService)
                .clearExpiredSubscription(
                        target
                );
    }

    @Test
    @DisplayName("만료 구독 정리에 실패해도 다음 대상의 알림 발송을 계속한다")
    void sendDiaryReminders_cleanupFailure_continuesNextTarget() {
        LocalDate today =
                LocalDate.of(2026, 8, 4);

        WebPushTarget expiredTarget =
                new WebPushTarget(
                        10L,
                        1L,
                        "https://example.com/expired",
                        "expired-p256dh",
                        "expired-auth"
                );

        WebPushTarget nextTarget =
                new WebPushTarget(
                        20L,
                        2L,
                        "https://example.com/next",
                        "next-p256dh",
                        "next-auth"
                );

        given(
                notificationTargetReader.findDiaryReminderTargets(
                        today
                )
        ).willReturn(
                List.of(
                        expiredTarget,
                        nextTarget
                )
        );

        willThrow(new PushSubscriptionExpiredException())
                .given(webPushService)
                .send(
                        expiredTarget,
                        "오늘의 회고를 남겨볼까요?",
                        "오늘 하루를 영어로 천천히 돌아보세요.",
                        "/home"
                );

        willThrow(new IllegalStateException("DB 정리 실패"))
                .given(pushSubscriptionCleanupService)
                .clearExpiredSubscription(expiredTarget);

        notificationService.sendDiaryReminders(today);

        verify(pushSubscriptionCleanupService)
                .clearExpiredSubscription(expiredTarget);

        verify(webPushService)
                .send(
                        nextTarget,
                        "오늘의 회고를 남겨볼까요?",
                        "오늘 하루를 영어로 천천히 돌아보세요.",
                        "/home"
                );
    }
}