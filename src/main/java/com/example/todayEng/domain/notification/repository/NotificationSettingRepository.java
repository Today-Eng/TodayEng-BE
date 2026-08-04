package com.example.todayEng.domain.notification.repository;

import com.example.todayEng.domain.notification.dto.WebPushTarget;
import com.example.todayEng.domain.notification.entity.NotificationSetting;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface NotificationSettingRepository
        extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findByUserId(Long userId);

    void deleteAllByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select notificationSetting
        from NotificationSetting notificationSetting
        where notificationSetting.pushEndpoint = :pushEndpoint
          and notificationSetting.user.id <> :currentUserId
        """)
    Optional<NotificationSetting> findOtherEndpointOwnerForUpdate(
            @Param("pushEndpoint")
            String pushEndpoint,

            @Param("currentUserId")
            Long currentUserId
    );

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            update NotificationSetting notificationSetting
            set notificationSetting.pushEndpoint = null,
                notificationSetting.p256dhKey = null,
                notificationSetting.authKey = null
            where notificationSetting.id = :notificationSettingId
              and notificationSetting.pushEndpoint = :pushEndpoint
              and notificationSetting.p256dhKey = :p256dhKey
              and notificationSetting.authKey = :authKey
            """)
    int clearPushSubscriptionIfMatches(
            @Param("notificationSettingId")
            Long notificationSettingId,

            @Param("pushEndpoint")
            String pushEndpoint,

            @Param("p256dhKey")
            String p256dhKey,

            @Param("authKey")
            String authKey
    );

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
