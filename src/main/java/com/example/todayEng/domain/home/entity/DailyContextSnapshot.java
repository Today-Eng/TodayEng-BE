package com.example.todayEng.domain.home.entity;

import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.home.entity.enums.DailyContextCollectionStatus;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.global.common.BaseTimeEntity;
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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "daily_context_snapshot",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_context_snapshot_user_date_type",
                columnNames = {"user_id", "context_date", "context_type"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyContextSnapshot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_context_snapshot_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "context_date", nullable = false)
    private LocalDate contextDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 30)
    private DiaryContextType contextType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_data", columnDefinition = "json")
    private JsonNode contextData;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_status", nullable = false, length = 20)
    private DailyContextCollectionStatus collectionStatus;

    @Column(name = "lease_version", nullable = false)
    private long leaseVersion;

    private DailyContextSnapshot(
            User user,
            LocalDate contextDate,
            DiaryContextType contextType
    ) {
        this.user = user;
        this.contextDate = contextDate;
        this.contextType = contextType;
        this.collectionStatus = DailyContextCollectionStatus.IN_PROGRESS;
        this.leaseVersion = 0L;
    }

    public static DailyContextSnapshot start(
            User user,
            LocalDate contextDate,
            DiaryContextType contextType
    ) {
        return new DailyContextSnapshot(user, contextDate, contextType);
    }

    public void succeed(JsonNode contextData) {
        this.contextData = contextData;
        this.collectionStatus = DailyContextCollectionStatus.SUCCEEDED;
    }

    public void fail() {
        this.contextData = null;
        this.collectionStatus = DailyContextCollectionStatus.FAILED;
    }

    public void attachData(JsonNode contextData) {
        this.contextData = contextData;
    }
}
