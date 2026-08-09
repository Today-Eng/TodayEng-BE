package com.example.todayEng.domain.notification.scheduler;

import com.example.todayEng.domain.notification.service.NotificationService;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryReminderScheduler {

    private final NotificationService notificationService;
    @Value("${notification.diary-reminder.zone:Asia/Seoul}")
    private String reminderZone;

    @Scheduled(
            cron = "${notification.diary-reminder.cron:0 0 22 * * *}",
            zone = "${notification.diary-reminder.zone:Asia/Seoul}"
    )
    public void sendDiaryReminders() {
        LocalDate today =
                LocalDate.now(ZoneId.of(reminderZone));

        log.info(
                "회고 알림 스케줄러 시작. date={}",
                today
        );

        notificationService.sendDiaryReminders(today);

        log.info(
                "회고 알림 스케줄러 종료. date={}",
                today
        );
    }
}
