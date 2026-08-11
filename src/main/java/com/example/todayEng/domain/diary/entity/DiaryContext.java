package com.example.todayEng.domain.diary.entity;

import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "diary_context",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_diary_context_diary_type_key",
                        columnNames = {"diary_id", "context_type", "context_key"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "context_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 30)
    private DiaryContextType contextType;

    @Column(name = "context_key", nullable = false)
    private int contextKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_data", columnDefinition = "json")
    private JsonNode contextData;

    @Column(name = "is_success", nullable = false)
    private boolean success;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private DiaryContext(
            Diary diary,
            DiaryContextType contextType,
            int contextKey,
            JsonNode contextData,
            boolean success
    ) {
        this.diary = diary;
        this.contextType = contextType;
        this.contextKey = contextKey;
        this.contextData = contextData;
        this.success = success;
    }

    public static DiaryContext success(
            Diary diary,
            DiaryContextType contextType,
            JsonNode contextData
    ) {
        return success(diary, contextType, 0, contextData);
    }

    public static DiaryContext success(
            Diary diary,
            DiaryContextType contextType,
            int contextKey,
            JsonNode contextData
    ) {
        return DiaryContext.builder()
                .diary(diary)
                .contextType(contextType)
                .contextKey(contextKey)
                .contextData(contextData)
                .success(true)
                .build();
    }

    public static DiaryContext failure(
            Diary diary,
            DiaryContextType contextType
    ) {
        return DiaryContext.builder()
                .diary(diary)
                .contextType(contextType)
                .contextKey(0)
                .success(false)
                .build();
    }

    public void updateContextData(JsonNode contextData) {
        this.contextData = contextData;
        this.success = true;
    }

    public void markFailed() {
        this.contextData = null;
        this.success = false;
    }
}
