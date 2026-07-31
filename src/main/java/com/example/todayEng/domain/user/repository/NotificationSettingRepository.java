package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    void deleteAllByUserId(Long userId);
}
