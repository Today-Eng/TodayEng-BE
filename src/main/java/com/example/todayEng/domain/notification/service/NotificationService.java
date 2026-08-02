package com.example.todayEng.domain.notification.service;

import com.example.todayEng.domain.notification.entity.NotificationSetting;
import com.example.todayEng.domain.notification.exception.PushSubscriptionExpiredException;
import com.example.todayEng.domain.notification.repository.NotificationSettingRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final WebPushService webPushService;

    @Transactional
    public void sendTestNotification(Long userId) {
        NotificationSetting notificationSetting =
                notificationSettingRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new BaseException(
                                        ErrorCode.PUSH_SUBSCRIPTION_REQUIRED
                                )
                        );

        if (!notificationSetting.hasPushSubscription()) {
            throw new BaseException(
                    ErrorCode.PUSH_SUBSCRIPTION_REQUIRED
            );
        }

        if (!notificationSetting.isUseEnabled()) {
            throw new BaseException(
                    ErrorCode.NOTIFICATION_DISABLED
            );
        }

        webPushService.send(
                notificationSetting,
                "TodayEng",
                "테스트 알림입니다.",
                "/home"
        );
    }

    @Transactional
    public void sendDiaryReminders(LocalDate today) {
        List<NotificationSetting> targets =
                notificationSettingRepository.findDiaryReminderTargets(
                        today
                );

        log.info(
                "회고 알림 발송 대상 수. count={}",
                targets.size()
        );

        for (NotificationSetting target : targets) {
            try {
                webPushService.send(
                        target,
                        "오늘의 회고를 남겨볼까요?",
                        "오늘 하루를 영어로 천천히 돌아보세요.",
                        "/home"
                );
            } catch (PushSubscriptionExpiredException exception) {
                target.clearPushSubscription();

                log.info(
                        "만료된 푸시 구독 제거. notificationSettingId={}, userId={}",
                        target.getId(),
                        target.getUser().getId()
                );
            } catch (Exception exception) {
                log.error(
                        "회고 알림 발송 실패. notificationSettingId={}, userId={}",
                        target.getId(),
                        target.getUser().getId(),
                        exception
                );
            }
        }
    }
}