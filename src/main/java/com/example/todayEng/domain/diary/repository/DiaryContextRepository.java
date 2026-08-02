package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryContextRepository extends JpaRepository<DiaryContext, Long> {

    Optional<DiaryContext> findByDiaryAndContextType(
            Diary diary,
            DiaryContextType contextType
    );
}
