package com.example.todayEng.domain.diary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@Table(
        name = "diary_context_source",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_diary_context_source_context_source_diary",
                        columnNames = {"context_id", "source_diary_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryContextSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_context_source_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "context_id", nullable = false)
    private DiaryContext context;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_diary_id", nullable = false)
    private Diary sourceDiary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private DiaryContextSource(
            DiaryContext context,
            Diary sourceDiary
    ) {
        this.context = context;
        this.sourceDiary = sourceDiary;
    }

    public static DiaryContextSource create(
            DiaryContext context,
            Diary sourceDiary
    ) {
        return DiaryContextSource.builder()
                .context(context)
                .sourceDiary(sourceDiary)
                .build();
    }
}
