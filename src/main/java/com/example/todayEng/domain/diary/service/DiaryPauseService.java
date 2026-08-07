package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.response.DiaryPauseResponse;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryPauseService {

    private final DiaryRepository diaryRepository;

    @Transactional
    public DiaryPauseResponse pause(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findByIdForUpdate(diaryId)
                .orElseThrow(() -> new BaseException(ErrorCode.DIARY_NOT_FOUND));

        if (!diary.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.ACCESS_DENIED);
        }
        if (diary.getStatus() == DiaryStatus.COMPLETED) {
            return DiaryPauseResponse.from(diary);
        }
        if (diary.getStatus() != DiaryStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.DIARY_NOT_IN_PROGRESS);
        }

        // 명시적인 중간 종료만 답변 개수와 무관하게 완료한다.
        // 화면 이탈 시에는 이 API가 호출되지 않으므로 IN_PROGRESS가 유지된다.
        diary.complete();
        return DiaryPauseResponse.from(diary);
    }
}
