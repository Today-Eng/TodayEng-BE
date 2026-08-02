package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.user.entity.User;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

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
}
