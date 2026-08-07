package com.example.todayEng.domain.diary.entity;

import com.example.todayEng.domain.diary.entity.enums.DiaryContextCollectionStatus;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.entity.enums.ReflectionQuestionGenerationStatus;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.global.common.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "question_generation_status", nullable = false, length = 20)
    @ColumnDefault("'NOT_STARTED'")
    private ReflectionQuestionGenerationStatus questionGenerationStatus;

    @Column(name = "question_generation_claimed_at")
    private LocalDateTime questionGenerationClaimedAt;

    @Column(name = "question_generation_lease_version", nullable = false)
    @ColumnDefault("0")
    private long questionGenerationLeaseVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_collection_status", nullable = false, length = 20)
    @ColumnDefault("'NOT_STARTED'")
    private DiaryContextCollectionStatus contextCollectionStatus;

    @Column(name = "context_collection_claimed_at")
    private LocalDateTime contextCollectionClaimedAt;

    @Column(name = "context_collection_lease_version", nullable = false)
    @ColumnDefault("0")
    private long contextCollectionLeaseVersion;

    @Column(name = "memo", length = 2000)
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
        this.questionGenerationStatus = ReflectionQuestionGenerationStatus.NOT_STARTED;
        this.questionGenerationLeaseVersion = 0L;
        this.contextCollectionStatus = DiaryContextCollectionStatus.NOT_STARTED;
        this.contextCollectionLeaseVersion = 0L;
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
        this.memo = normalizeMemo(memo);
    }

    public void complete() {
        if (this.status == DiaryStatus.COMPLETED) {
            return;
        }

        this.status = DiaryStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void complete(String finalMemo) {
        this.memo = normalizeMemo(finalMemo);
        complete();
    }

    public void delete() {
        if (this.status != DiaryStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only a completed diary can be deleted."
            );
        }

        this.status = DiaryStatus.DELETED;
        this.memo = null;
    }

    public void completeQuestionGeneration() {
        this.questionGenerationStatus = ReflectionQuestionGenerationStatus.COMPLETED;
    }

    public void failQuestionGeneration() {
        this.questionGenerationStatus = ReflectionQuestionGenerationStatus.FAILED;
    }

    private String normalizeMemo(String memo) {
        if (memo == null || memo.isBlank()) {
            return null;
        }
        return memo.strip();
    }
}
