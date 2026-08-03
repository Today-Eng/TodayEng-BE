package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.response.DiaryPauseResponse;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryPauseService {

    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final long RESUME_WINDOW_HOURS = 24;

    private final DiaryRepository diaryRepository;

    @Transactional
    public DiaryPauseResponse pause(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findByIdForUpdate(diaryId)
                .orElseThrow(() -> new BaseException(ErrorCode.DIARY_NOT_FOUND));

        if (!diary.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.ACCESS_DENIED);
        }
        if (diary.getStatus() == DiaryStatus.PAUSED) {
            throw new BaseException(ErrorCode.DIARY_ALREADY_PAUSED);
        }
        if (diary.getStatus() != DiaryStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.DIARY_NOT_IN_PROGRESS);
        }

        LocalDateTime pausedAt = LocalDateTime.now(SERVICE_ZONE_ID);
        diary.pause(pausedAt, pausedAt.plusHours(RESUME_WINDOW_HOURS));
        return DiaryPauseResponse.from(diary);
    }
}
