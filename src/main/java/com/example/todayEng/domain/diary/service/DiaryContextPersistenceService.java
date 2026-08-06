package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryContextRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryContextPersistenceService {

    private final DiaryRepository diaryRepository;
    private final DiaryContextRepository contextRepository;

    @Transactional
    public DiaryContext saveMemo(Long userId, Long diaryId, String memo, JsonNode data) {
        Diary diary = getManagedDiary(userId, diaryId);
        diary.updateMemo(memo);
        return saveSuccess(diary, DiaryContextType.MEMO, data);
    }

    @Transactional
    public DiaryContext saveSuccess(Long userId, Long diaryId,
            DiaryContextType type, JsonNode data) {
        return saveSuccess(getManagedDiary(userId, diaryId), type, data);
    }

    @Transactional
    public DiaryContext saveFailure(Long userId, Long diaryId, DiaryContextType type) {
        Diary diary = getManagedDiary(userId, diaryId);
        DiaryContext context = contextRepository.findByDiaryAndContextType(diary, type)
                .orElseGet(() -> DiaryContext.failure(diary, type));
        context.markFailed();
        return contextRepository.save(context);
    }

    private DiaryContext saveSuccess(Diary diary, DiaryContextType type, JsonNode data) {
        DiaryContext context = contextRepository.findByDiaryAndContextType(diary, type)
                .orElseGet(() -> DiaryContext.success(diary, type, data));
        context.updateContextData(data);
        return contextRepository.save(context);
    }

    private Diary getManagedDiary(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findByIdAndUserId(diaryId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENTITY_NOT_FOUND));
        if (diary.getStatus() != DiaryStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.DIARY_ALREADY_COMPLETED);
        }
        return diary;
    }
}
