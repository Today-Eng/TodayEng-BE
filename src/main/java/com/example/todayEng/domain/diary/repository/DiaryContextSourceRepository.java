package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.DiaryContextSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryContextSourceRepository
        extends JpaRepository<DiaryContextSource, Long> {

    void deleteAllByContextId(Long contextId);
}
