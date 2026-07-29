package com.example.todayEng.domain.diary.entity;

import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.global.common.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "diary",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_diary_user_date",
                        columnNames = {"user_id", "diary_date"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_diary_user")
    )
    private User user;

    @Column(name = "diary_date", nullable = false)
    private LocalDate diaryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DiaryStatus status;

    @Column(name = "memo", length = 200)
    private String memo;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Diary(
            User user,
            LocalDate diaryDate
    ) {
        this.user = user;
        this.diaryDate = diaryDate;
        this.status = DiaryStatus.IN_PROGRESS;
    }

    public static Diary create(
            User user,
            LocalDate diaryDate
    ) {
        return Diary.builder()
                .user(user)
                .diaryDate(diaryDate)
                .build();
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void complete() {
        if (this.status == DiaryStatus.COMPLETED) {
            return;
        }

        this.status = DiaryStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}