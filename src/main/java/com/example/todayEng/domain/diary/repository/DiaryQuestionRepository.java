package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryQuestionRepository extends JpaRepository<DiaryQuestion, Long> {

    List<DiaryQuestion> findAllByDiaryIdOrderByQuestionOrder(Long diaryId);
}
