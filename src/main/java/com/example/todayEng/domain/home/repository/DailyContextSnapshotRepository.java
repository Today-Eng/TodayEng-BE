package com.example.todayEng.domain.home.repository;

import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.home.entity.DailyContextSnapshot;
import com.example.todayEng.domain.home.entity.enums.DailyContextCollectionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyContextSnapshotRepository
        extends JpaRepository<DailyContextSnapshot, Long> {

    List<DailyContextSnapshot> findAllByUserIdAndContextDateAndCollectionStatus(
            Long userId,
            LocalDate contextDate,
            DailyContextCollectionStatus collectionStatus
    );

    Optional<DailyContextSnapshot> findByUser_IdAndContextDateAndContextType(
            Long userId,
            LocalDate contextDate,
            DiaryContextType contextType
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE DailyContextSnapshot s
            SET s.updatedAt = :now, s.leaseVersion = s.leaseVersion + 1
            WHERE s.user.id = :userId
              AND s.contextDate = :contextDate
              AND s.contextType = :contextType
              AND s.collectionStatus = :inProgress
              AND s.updatedAt < :staleBefore
            """)
    int reclaimStale(
            @Param("userId") Long userId,
            @Param("contextDate") LocalDate contextDate,
            @Param("contextType") DiaryContextType contextType,
            @Param("inProgress") DailyContextCollectionStatus inProgress,
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE DailyContextSnapshot s
            SET s.collectionStatus = :targetStatus, s.leaseVersion = s.leaseVersion + 1
            WHERE s.id = :id
              AND s.collectionStatus = :inProgress
              AND s.leaseVersion = :expectedLeaseVersion
            """)
    int finishIfOwned(
            @Param("id") Long id,
            @Param("targetStatus") DailyContextCollectionStatus targetStatus,
            @Param("inProgress") DailyContextCollectionStatus inProgress,
            @Param("expectedLeaseVersion") long expectedLeaseVersion
    );
}
