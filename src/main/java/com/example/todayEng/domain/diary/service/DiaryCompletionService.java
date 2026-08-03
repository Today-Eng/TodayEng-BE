package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.request.DiaryCompleteRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryCompleteResponse;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryAnswerRepository;
import com.example.todayEng.domain.diary.repository.DiaryQuestionRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryCompletionService {
    private final DiaryRepository diaryRepository;
    private final DiaryQuestionRepository questionRepository;
    private final DiaryAnswerRepository answerRepository;
    private final DiaryCompletionPolicy completionPolicy;

    @Transactional
    public DiaryCompleteResponse complete(Long userId, Long diaryId, DiaryCompleteRequest request) {
        Diary diary = diaryRepository.findByIdForUpdate(diaryId)
                .orElseThrow(() -> new BaseException(ErrorCode.DIARY_NOT_FOUND));
        if (!diary.getUser().getId().equals(userId)) throw new BaseException(ErrorCode.ACCESS_DENIED);
        // Diary.complete()의 기존 멱등 정책을 유지하며 재요청에서 기존 memo를 변경하지 않는다.
        if (diary.getStatus() == DiaryStatus.COMPLETED) return DiaryCompleteResponse.from(diary);
        var questions = questionRepository.findAllByDiaryIdInReflectionOrder(diaryId);
        var answers = answerRepository.findAllByDiaryIdInReflectionOrder(diaryId);
        completionPolicy.validate(questions, answers);
        diary.complete(request.finalMemo());
        return DiaryCompleteResponse.from(diary);
    }
}
