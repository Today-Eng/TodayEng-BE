package com.example.todayEng.domain.notification.service;

import com.example.todayEng.domain.notification.dto.WebPushTarget;
import com.example.todayEng.domain.notification.repository.NotificationSettingRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationTargetReader {

    private final NotificationSettingRepository notificationSettingRepository;

    @Transactional(readOnly = true)
    public List<WebPushTarget> findDiaryReminderTargets(
            LocalDate today
    ) {
        return notificationSettingRepository
                .findDiaryReminderTargets(today);
    }
}
