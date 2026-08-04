package com.example.todayEng.domain.notification.service;

import com.example.todayEng.domain.notification.dto.WebPushTarget;
import com.example.todayEng.domain.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushSubscriptionCleanupService {

    private final NotificationSettingRepository notificationSettingRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearExpiredSubscription(
            WebPushTarget target
    ) {
        int updatedCount =
                notificationSettingRepository
                        .clearPushSubscriptionIfMatches(
                                target.notificationSettingId(),
                                target.pushEndpoint(),
                                target.p256dhKey(),
                                target.authKey()
                        );

        if (updatedCount == 0) {
            log.info(
                    "구독 정보가 이미 변경되어 만료 구독 제거를 생략했습니다. notificationSettingId={}, userId={}",
                    target.notificationSettingId(),
                    target.userId()
            );
        }
    }
}