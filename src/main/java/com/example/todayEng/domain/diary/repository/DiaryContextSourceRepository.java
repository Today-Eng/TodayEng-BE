package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.DiaryContextSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaryContextSourceRepository
        extends JpaRepository<DiaryContextSource, Long> {

    void deleteAllByContextId(Long contextId);

    @Modifying
    @Query("DELETE FROM DiaryContextSource s WHERE s.sourceDiary.id = :diaryId")
    void deleteAllBySourceDiaryId(@Param("diaryId") Long diaryId);

    @Modifying
    @Query("DELETE FROM DiaryContextSource s WHERE s.context.diary.id = :diaryId")
    void deleteAllByContextDiaryId(@Param("diaryId") Long diaryId);
}
