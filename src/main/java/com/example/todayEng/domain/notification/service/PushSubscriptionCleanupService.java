package com.example.todayEng.domain.notification.service;

import com.example.todayEng.domain.notification.entity.NotificationSetting;
import com.example.todayEng.domain.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushSubscriptionCleanupService {

    private final NotificationSettingRepository notificationSettingRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearExpiredSubscription(
            Long notificationSettingId
    ) {
        notificationSettingRepository
                .findById(notificationSettingId)
                .ifPresent(
                        NotificationSetting::clearPushSubscription
                );
    }
}
