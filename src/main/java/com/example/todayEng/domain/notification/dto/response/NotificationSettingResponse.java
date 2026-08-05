package com.example.todayEng.domain.notification.dto.response;

import com.example.todayEng.domain.notification.entity.NotificationSetting;

public record NotificationSettingResponse(
        boolean isEnabled,
        boolean hasPushSubscription
) {

    public static NotificationSettingResponse from(
            NotificationSetting notificationSetting
    ) {
        return new NotificationSettingResponse(
                notificationSetting.isUseEnabled(),
                notificationSetting.hasPushSubscription()
        );
    }

    public static NotificationSettingResponse disabled() {
        return new NotificationSettingResponse(
                false,
                false
        );
    }
}
