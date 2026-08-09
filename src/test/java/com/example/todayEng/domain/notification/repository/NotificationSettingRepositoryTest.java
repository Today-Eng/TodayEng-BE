package com.example.todayEng.domain.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.notification.dto.WebPushTarget;
import com.example.todayEng.domain.notification.entity.NotificationSetting;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class NotificationSettingRepositoryTest {

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Test
    void findDiaryReminderTargets_excludesUserWithDeletedDiaryToday() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        User user = userRepository.save(User.create());
        createEnabledNotificationSetting(user);

        Diary diary = diaryRepository.saveAndFlush(Diary.create(user, today));
        diary.complete();
        diary.delete();
        diaryRepository.saveAndFlush(diary);

        List<WebPushTarget> targets =
                notificationSettingRepository.findDiaryReminderTargets(today);

        assertThat(targets).isEmpty();
    }

    @Test
    void findDiaryReminderTargets_includesUserWithInProgressDiaryToday() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        User user = userRepository.save(User.create());
        createEnabledNotificationSetting(user);
        diaryRepository.saveAndFlush(Diary.create(user, today));

        List<WebPushTarget> targets =
                notificationSettingRepository.findDiaryReminderTargets(today);

        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).userId()).isEqualTo(user.getId());
    }

    private void createEnabledNotificationSetting(User user) {
        NotificationSetting setting = NotificationSetting.create(user);
        setting.updatePushSubscription(
                "https://example.com/push/" + user.getId(),
                "p256dh-key-" + user.getId(),
                "auth-key-" + user.getId()
        );
        setting.enable();
        notificationSettingRepository.saveAndFlush(setting);
    }
}
