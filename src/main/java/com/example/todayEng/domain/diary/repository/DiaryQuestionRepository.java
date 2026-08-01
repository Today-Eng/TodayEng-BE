package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryQuestionRepository
        extends JpaRepository<DiaryQuestion, Long> {

    List<DiaryQuestion>
    findAllByDiaryIdInAndQuestionTypeOrderByDiaryIdAscQuestionOrderAsc(
            List<Long> diaryIds,
            QuestionType questionType
    );

    List<DiaryQuestion>
    findAllByDiaryIdOrderByQuestionOrderAscIdAsc(
            Long diaryId
    );
}
