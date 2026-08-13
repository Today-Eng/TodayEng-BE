package com.example.todayEng.domain.notification.service;

import com.example.todayEng.domain.notification.dto.WebPushTarget;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationTargetReader notificationTargetReader;
    private final WebPushService webPushService;
    private final PushSubscriptionCleanupService pushSubscriptionCleanupService;

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

        log.info(
                "테스트 알림 발송 성공. notificationSettingId={}, userId={}",
                notificationSetting.getId(),
                userId
        );
    }

    public void sendDiaryReminders(LocalDate today) {
        List<WebPushTarget> targets =
                notificationTargetReader.findDiaryReminderTargets(
                        today
                );

        log.info(
                "회고 알림 발송 대상 수. count={}",
                targets.size()
        );

        for (WebPushTarget target : targets) {
            try {
                webPushService.send(
                        target,
                        "오늘의 회고를 남겨볼까요?",
                        "오늘 하루를 영어로 천천히 돌아보세요.",
                        "/home"
                );

                log.info(
                        "회고 알림 발송 성공. notificationSettingId={}, userId={}",
                        target.notificationSettingId(),
                        target.userId()
                );
            } catch (PushSubscriptionExpiredException exception) {
                try {
                    pushSubscriptionCleanupService.clearExpiredSubscription(
                            target
                    );

                    log.info(
                            "만료된 푸시 구독 정리 완료. notificationSettingId={}, userId={}",
                            target.notificationSettingId(),
                            target.userId()
                    );
                } catch (Exception cleanupException) {
                    log.error(
                            "만료된 푸시 구독 정리 실패. notificationSettingId={}, userId={}",
                            target.notificationSettingId(),
                            target.userId(),
                            cleanupException
                    );
                }
            } catch (Exception exception) {
                log.error(
                        "회고 알림 발송 실패. notificationSettingId={}, userId={}",
                        target.notificationSettingId(),
                        target.userId(),
                        exception
                );
            }
        }
    }
}
