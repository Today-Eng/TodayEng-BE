package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.request.DiaryMemoUpdateRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryMemoUpdateResponse;
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
public class DiaryMemoService {

    private final DiaryRepository diaryRepository;

    @Transactional
    public DiaryMemoUpdateResponse updateMemo(
            Long userId,
            Long diaryId,
            DiaryMemoUpdateRequest request
    ) {
        Diary diary = diaryRepository.findByIdForUpdate(diaryId)
                .orElseThrow(() -> new BaseException(ErrorCode.DIARY_NOT_FOUND));

        if (!diary.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.ACCESS_DENIED);
        }

        if (diary.getStatus() != DiaryStatus.COMPLETED) {
            throw new BaseException(ErrorCode.DIARY_NOT_COMPLETED);
        }

        diary.updateMemo(request.memo());
        return DiaryMemoUpdateResponse.from(diary);
    }
}
