package com.example.todayEng.domain.notification.repository;

import com.example.todayEng.domain.notification.entity.NotificationSetting;
import com.example.todayEng.domain.notification.dto.WebPushTarget;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationSettingRepository
        extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findByUserId(Long userId);

    void deleteAllByUserId(Long userId);

    @Query("""
        select new com.example.todayEng.domain.notification.dto.WebPushTarget(
            notificationSetting.id,
            notificationSetting.user.id,
            notificationSetting.pushEndpoint,
            notificationSetting.p256dhKey,
            notificationSetting.authKey
        )
        from NotificationSetting notificationSetting
        where notificationSetting.useEnabled = true
          and notificationSetting.pushEndpoint is not null
          and notificationSetting.p256dhKey is not null
          and notificationSetting.authKey is not null
          and not exists (
              select diary.id
              from Diary diary
              where diary.user = notificationSetting.user
                and diary.diaryDate = :today
                and diary.status =
                    com.example.todayEng.domain.diary.entity.enums.DiaryStatus.COMPLETED
          )
        """)
    List<WebPushTarget> findDiaryReminderTargets(
            @Param("today") LocalDate today
    );
}
