package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryAnswerRepository
        extends JpaRepository<DiaryAnswer, Long> {

    List<DiaryAnswer> findAllByQuestionIdIn(
            List<Long> questionIds
    );
}
