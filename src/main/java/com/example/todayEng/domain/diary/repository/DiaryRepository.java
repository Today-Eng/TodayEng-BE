package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.user.entity.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    Optional<Diary> findByUserAndDiaryDate(
            User user,
            LocalDate diaryDate
    );

    List<Diary>
    findAllByUserIdAndStatusAndDiaryDateBetweenOrderByDiaryDateDesc(
            Long userId,
            DiaryStatus status,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<Diary> findByIdAndUserIdAndStatus(
            Long diaryId,
            Long userId,
            DiaryStatus status
    );
}
