package com.example.todayEng.domain.diary.entity;

import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_id")
    private Long id;

    /*
     * User 엔티티 구현 후 수정
     *
     * @ManyToOne(fetch = FetchType.LAZY)
     * @JoinColumn(name = "user_id", nullable = false)
     * private User user;
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "diary_date", nullable = false)
    private LocalDate diaryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DiaryStatus status;

    @Column(name = "memo", length = 200)
    private String memo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Diary(
            Long userId,
            LocalDate diaryDate
    ) {
        this.userId = userId;
        this.diaryDate = diaryDate;
        this.status = DiaryStatus.IN_PROGRESS;
    }

    public static Diary create(
            Long userId,
            LocalDate diaryDate
    ) {
        return Diary.builder()
                .userId(userId)
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