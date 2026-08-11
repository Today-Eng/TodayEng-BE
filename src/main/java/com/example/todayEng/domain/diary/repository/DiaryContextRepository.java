package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaryContextRepository extends JpaRepository<DiaryContext, Long> {

    List<DiaryContext> findAllByDiaryIdAndSuccessTrueOrderById(Long diaryId);

    List<DiaryContext> findAllByDiaryIdAndIdIn(Long diaryId, List<Long> ids);

    Optional<DiaryContext> findByDiaryAndContextType(
            Diary diary,
            DiaryContextType contextType
    );

    Optional<DiaryContext> findByDiaryAndContextTypeAndContextKey(
            Diary diary,
            DiaryContextType contextType,
            int contextKey
    );

    List<DiaryContext> findAllByDiaryAndContextTypeOrderByContextKey(
            Diary diary,
            DiaryContextType contextType
    );

    @Modifying
    @Query("DELETE FROM DiaryContext c WHERE c.diary.id = :diaryId")
    void deleteAllByDiaryId(@Param("diaryId") Long diaryId);

}
