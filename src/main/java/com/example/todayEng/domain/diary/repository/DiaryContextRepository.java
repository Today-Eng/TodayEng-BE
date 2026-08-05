package com.example.todayEng.domain.diary.repository;

import java.util.List;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryContextRepository extends JpaRepository<DiaryContext, Long> {

    List<DiaryContext> findAllByDiaryIdAndSuccessTrueOrderById(Long diaryId);

    List<DiaryContext> findAllByDiaryIdAndIdIn(Long diaryId, List<Long> ids);

    Optional<DiaryContext> findByDiaryAndContextType(
            Diary diary,
            DiaryContextType contextType
    );

}
