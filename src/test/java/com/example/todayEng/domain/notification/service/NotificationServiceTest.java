package com.example.todayEng.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.example.todayEng.domain.notification.entity.NotificationSetting;
import com.example.todayEng.domain.notification.exception.PushSubscriptionExpiredException;
import com.example.todayEng.domain.notification.repository.NotificationSettingRepository;
import com.example.todayEng.domain.user.entity.User;
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
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private WebPushService webPushService;

    @InjectMocks
    private NotificationService notificationService;

    private final User user = User.create();

    @Test
    @DisplayName("회고 알림 발송 중 구독이 만료되면 구독 정보만 제거한다")
    void sendDiaryReminders_expiredSubscription_clearsSubscription() {
        LocalDate today =
                LocalDate.of(2026, 8, 2);

        NotificationSetting setting =
                NotificationSetting.create(user);

        setting.updatePushSubscription(
                "https://example.com/push",
                "p256dh-key",
                "auth-key"
        );

        given(notificationSettingRepository.findDiaryReminderTargets(today))
                .willReturn(List.of(setting));

        willThrow(new PushSubscriptionExpiredException())
                .given(webPushService)
                .send(
                        setting,
                        "오늘의 회고를 남겨볼까요?",
                        "오늘 하루를 영어로 천천히 돌아보세요.",
                        "/home"
                );

        notificationService.sendDiaryReminders(today);

        assertThat(setting.isUseEnabled()).isTrue();
        assertThat(setting.hasPushSubscription()).isFalse();
        assertThat(setting.getPushEndpoint()).isNull();
        assertThat(setting.getP256dhKey()).isNull();
        assertThat(setting.getAuthKey()).isNull();
    }
}
