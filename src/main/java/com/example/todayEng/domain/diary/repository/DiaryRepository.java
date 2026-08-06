package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.user.entity.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Diary d where d.id = :diaryId")
    Optional<Diary> findByIdForUpdate(@Param("diaryId") Long diaryId);

    boolean existsByIdAndUserId(Long diaryId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Diary d
               set d.questionGenerationStatus = com.example.todayEng.domain.diary.entity.enums.ReflectionQuestionGenerationStatus.GENERATING
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
            @Param("userId") Long userId
    );

    Optional<Diary> findByIdAndUserId(Long diaryId, Long userId);

    Optional<Diary> findByUserAndDiaryDate(
            User user,
            LocalDate diaryDate
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
}
