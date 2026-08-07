package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Diary d where d.id = :diaryId")
    Optional<Diary> findByIdForUpdate(@Param("diaryId") Long diaryId);

    boolean existsByIdAndUserId(Long diaryId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Diary d
               set d.questionGenerationStatus = com.example.todayEng.domain.diary.entity.enums.ReflectionQuestionGenerationStatus.GENERATING,
                   d.questionGenerationClaimedAt = :now
             where d.id = :diaryId
               and d.user.id = :userId
               and d.status = com.example.todayEng.domain.diary.entity.enums.DiaryStatus.IN_PROGRESS
               and d.questionGenerationStatus in (
                   com.example.todayEng.domain.diary.entity.enums.ReflectionQuestionGenerationStatus.NOT_STARTED,
                   com.example.todayEng.domain.diary.entity.enums.ReflectionQuestionGenerationStatus.FAILED
               )
            """)
    int claimQuestionGeneration(
            @Param("diaryId") Long diaryId,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Diary d
               set d.questionGenerationClaimedAt = :now
             where d.id = :diaryId
               and d.user.id = :userId
               and d.status = com.example.todayEng.domain.diary.entity.enums.DiaryStatus.IN_PROGRESS
               and d.questionGenerationStatus = com.example.todayEng.domain.diary.entity.enums.ReflectionQuestionGenerationStatus.GENERATING
               and d.questionGenerationClaimedAt < :staleBefore
            """)
    int reclaimStaleQuestionGeneration(
            @Param("diaryId") Long diaryId,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Diary d
               set d.contextCollectionStatus = com.example.todayEng.domain.diary.entity.enums.DiaryContextCollectionStatus.COLLECTING,
                   d.contextCollectionClaimedAt = :now
             where d.id = :diaryId
               and d.user.id = :userId
               and d.status = com.example.todayEng.domain.diary.entity.enums.DiaryStatus.IN_PROGRESS
               and d.contextCollectionStatus in (
                   com.example.todayEng.domain.diary.entity.enums.DiaryContextCollectionStatus.NOT_STARTED,
                   com.example.todayEng.domain.diary.entity.enums.DiaryContextCollectionStatus.FAILED
               )
            """)
    int claimContextCollection(
            @Param("diaryId") Long diaryId,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Diary d
               set d.contextCollectionClaimedAt = :now
             where d.id = :diaryId
               and d.user.id = :userId
               and d.status = com.example.todayEng.domain.diary.entity.enums.DiaryStatus.IN_PROGRESS
               and d.contextCollectionStatus = com.example.todayEng.domain.diary.entity.enums.DiaryContextCollectionStatus.COLLECTING
               and d.contextCollectionClaimedAt < :staleBefore
            """)
    int reclaimStaleContextCollection(
            @Param("diaryId") Long diaryId,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore
    );

    Optional<Diary> findByIdAndUserId(Long diaryId, Long userId);

    Optional<Diary> findByUserAndDiaryDate(
            User user,
            LocalDate diaryDate
    );

    Optional<Diary> findByUserIdAndDiaryDate(Long userId, LocalDate diaryDate);

    List<Diary> findAllByUserIdAndDiaryDateBetweenOrderByDiaryDate(
            Long userId,
            LocalDate fromDate,
            LocalDate toDate
    );

    @Query("""
            select d from Diary d
            where d.user.id = :userId
              and d.status = com.example.todayEng.domain.diary.entity.enums.DiaryStatus.COMPLETED
              and d.id <> :currentDiaryId
              and d.diaryDate >= :fromDate
              and d.diaryDate < :currentDate
            order by d.diaryDate desc, d.id desc
            """)
    List<Diary> findRecentCompletedForMemory(
            @Param("userId") Long userId,
            @Param("currentDiaryId") Long currentDiaryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("currentDate") LocalDate currentDate,
            Pageable pageable
    );

    List<Diary> findAllByUserIdAndStatusAndDiaryDateBetweenOrderByDiaryDateDesc(
            Long userId,
            DiaryStatus status,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<Diary> findByIdAndUserIdAndStatus(
            Long diaryId,
            Long userId,
            DiaryStatus status
    );
}
