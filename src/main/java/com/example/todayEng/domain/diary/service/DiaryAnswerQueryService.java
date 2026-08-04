package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.response.*;
import com.example.todayEng.domain.diary.entity.Diary;
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
@Transactional(readOnly = true)
public class DiaryAnswerQueryService {
    private final DiaryRepository diaryRepository;
    private final DiaryQuestionRepository questionRepository;
    private final DiaryAnswerRepository answerRepository;

    public DiaryAnswerDetailResponse getAnswer(Long userId, Long diaryId, Long answerId) {
        validateOwner(userId, diaryId);
        var answer = answerRepository.findDetailById(answerId)
                .orElseThrow(() -> new BaseException(ErrorCode.ANSWER_NOT_FOUND));
        if (!answer.getQuestion().getDiary().getId().equals(diaryId)) {
            throw new BaseException(ErrorCode.ANSWER_NOT_FOUND);
        }
        return DiaryAnswerDetailResponse.from(answer);
    }

    public DiaryAnswerListResponse getAnswers(Long userId, Long diaryId) {
        validateOwner(userId, diaryId);
        var answers = answerRepository.findAllByDiaryIdInReflectionOrder(diaryId);
        return new DiaryAnswerListResponse(answers.size(), questionRepository.countByDiaryId(diaryId),
                answers.stream().map(DiaryAnswerSummaryResponse::from).toList());
    }

    private Diary validateOwner(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new BaseException(ErrorCode.DIARY_NOT_FOUND));
        if (!diary.getUser().getId().equals(userId)) throw new BaseException(ErrorCode.ACCESS_DENIED);
        return diary;
    }
}
