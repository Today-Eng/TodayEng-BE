package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.diary.sse.DiarySseEmitterManager;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class DiarySubscriptionService {

    private final DiaryRepository diaryRepository;
    private final DiarySseEmitterManager emitterManager;

    public SseEmitter subscribe(Long userId, Long diaryId) {
        if (!diaryRepository.existsByIdAndUserId(diaryId, userId)) {
            throw new BaseException(ErrorCode.ACCESS_DENIED);
        }

        return emitterManager.subscribe(userId, diaryId);
    }
}
