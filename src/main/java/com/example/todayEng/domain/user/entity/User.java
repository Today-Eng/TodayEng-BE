package com.example.todayEng.domain.user.entity;


import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.example.todayEng.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_uuid",
                        columnNames = "uuid"
                ),
                @UniqueConstraint(
                        name = "uk_users_nickname",
                        columnNames = "nickname"
                ),
                @UniqueConstraint(
                        name = "uk_users_email",
                        columnNames = "email"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "nickname", length = 30)
    private String nickname;

    @Column(name = "profile_url", length = 500)
    private String profileUrl;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "english_level", length = 30)
    private EnglishLevel englishLevel;

    @Column(name = "total_diary_count", nullable = false)
    private Long totalDiaryCount = 0L;

    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak = 0;

    private User(UUID uuid, String email) {
        this.uuid = uuid;
        this.email = email;
    }

    public static User create() {
        return new User(UUID.randomUUID(), null);
    }

    public static User create(String email) {
        return new User(UUID.randomUUID(), email);
    }

    public void completeOnboarding(
            String nickname,
            String profileUrl,
            EnglishLevel englishLevel
    ) {
        this.nickname = nickname;
        this.profileUrl = profileUrl;
        this.englishLevel = englishLevel;
    }

    public void updateProfile(String nickname, String profileUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }

        if (profileUrl != null) {
            this.profileUrl = profileUrl;
        }
    }

    public void updateEnglishLevel(EnglishLevel englishLevel) {
        this.englishLevel = englishLevel;
    }

    public boolean isOnboardingCompleted() {
        return nickname != null && englishLevel != null;
    }

    public void increaseDiaryCount() {
        this.totalDiaryCount++;
    }

    public void updateCurrentStreak(int currentStreak) {
        if (currentStreak < 0) {
            throw new IllegalArgumentException("연속 작성 일수는 0 이상이어야 합니다.");
        }

        this.currentStreak = currentStreak;
    }
}
