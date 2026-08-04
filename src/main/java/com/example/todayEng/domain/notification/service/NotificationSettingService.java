package com.example.todayEng.domain.notification.service;

import com.example.todayEng.domain.notification.dto.request.NotificationEnabledRequest;
import com.example.todayEng.domain.notification.dto.request.PushSubscriptionRequest;
import com.example.todayEng.domain.notification.dto.response.NotificationSettingResponse;
import com.example.todayEng.domain.notification.entity.NotificationSetting;
import com.example.todayEng.domain.notification.repository.NotificationSettingRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;

    public NotificationSettingResponse getNotificationSetting(
            Long userId
    ) {
        validateUserExists(userId);

        return notificationSettingRepository.findByUserId(userId)
                .map(NotificationSettingResponse::from)
                .orElseGet(NotificationSettingResponse::disabled);
    }

    @Transactional
    public NotificationSettingResponse savePushSubscription(
            Long userId,
            PushSubscriptionRequest request
    ) {
        User user = getUser(userId);

        NotificationSetting notificationSetting =
                notificationSettingRepository.findByUserId(userId)
                        .orElse(null);

        boolean isNewSetting =
                notificationSetting == null;

        if (isNewSetting) {
            notificationSetting =
                    NotificationSetting.create(user);
        }

        notificationSetting.updatePushSubscription(
                request.endpoint(),
                request.keys().p256dh(),
                request.keys().auth()
        );

        if (isNewSetting) {
            notificationSetting.enable();
        }

        NotificationSetting savedSetting =
                notificationSettingRepository.save(
                        notificationSetting
                );

        return NotificationSettingResponse.from(
                savedSetting
        );
    }

    @Transactional
    public NotificationSettingResponse updateNotificationEnabled(
            Long userId,
            NotificationEnabledRequest request
    ) {
        validateUserExists(userId);

        NotificationSetting notificationSetting =
                notificationSettingRepository.findByUserId(userId)
                        .orElse(null);

        if (notificationSetting == null) {
            if (request.isEnabled()) {
                throw new BaseException(
                        ErrorCode.PUSH_SUBSCRIPTION_REQUIRED
                );
            }

            return NotificationSettingResponse.disabled();
        }

        if (request.isEnabled()) {
            notificationSetting.enable();
        } else {
            notificationSetting.disable();
        }

        return NotificationSettingResponse.from(
                notificationSetting
        );
    }

    @Transactional
    public NotificationSettingResponse deletePushSubscription(
            Long userId
    ) {
        validateUserExists(userId);

        return notificationSettingRepository.findByUserId(userId)
                .map(notificationSetting -> {
                    notificationSetting.clearPushSubscription();

                    return NotificationSettingResponse.from(
                            notificationSetting
                    );
                })
                .orElseGet(NotificationSettingResponse::disabled);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new BaseException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BaseException(
                    ErrorCode.USER_NOT_FOUND
            );
        }
    }
}
