package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.DiaryContext;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryContextRepository extends JpaRepository<DiaryContext, Long> {

    List<DiaryContext> findAllByDiaryIdAndSuccessTrueOrderById(Long diaryId);

    List<DiaryContext> findAllByDiaryIdAndIdIn(Long diaryId, List<Long> ids);
}
