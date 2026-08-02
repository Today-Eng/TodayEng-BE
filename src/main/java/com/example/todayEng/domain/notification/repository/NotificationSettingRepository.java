package com.example.todayEng.domain.notification.repository;

import com.example.todayEng.domain.notification.entity.NotificationSetting;
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
            select notificationSetting
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
                    and diary.status = com.example.todayEng.domain.diary.entity.enums.DiaryStatus.COMPLETED
              )
            """)
    List<NotificationSetting> findDiaryReminderTargets(
            @Param("today") LocalDate today
    );
}
