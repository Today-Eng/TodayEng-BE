package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.event.DiaryAudioCleanupEvent;
import com.example.todayEng.domain.diary.repository.DiaryAnswerRepository;
import com.example.todayEng.domain.diary.repository.DiaryContextRepository;
import com.example.todayEng.domain.diary.repository.DiaryContextSourceRepository;
import com.example.todayEng.domain.diary.repository.DiaryQuestionRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryDeletionService {

    private final DiaryRepository diaryRepository;
    private final DiaryQuestionRepository questionRepository;
    private final DiaryAnswerRepository answerRepository;
    private final DiaryContextRepository contextRepository;
    private final DiaryContextSourceRepository contextSourceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void delete(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findByIdForUpdate(diaryId)
                .orElseThrow(() -> new BaseException(ErrorCode.DIARY_NOT_FOUND));

        if (!diary.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.ACCESS_DENIED);
        }
        if (diary.getStatus() == DiaryStatus.DELETED) {
            throw new BaseException(ErrorCode.DIARY_ALREADY_DELETED);
        }
        if (diary.getStatus() != DiaryStatus.COMPLETED) {
            throw new BaseException(ErrorCode.DIARY_DELETE_NOT_ALLOWED);
        }

        List<String> answerAudioKeys = answerRepository.findAudioKeysByDiaryId(diaryId);
        List<String> ttsAudioKeys = questionRepository.findTtsAudioKeysByDiaryId(diaryId);

        contextSourceRepository.deleteAllBySourceDiaryId(diaryId);
        contextSourceRepository.deleteAllByContextDiaryId(diaryId);
        answerRepository.deleteAllByDiaryId(diaryId);
        questionRepository.deleteFollowUpsByDiaryId(diaryId);
        questionRepository.deleteAllByDiaryId(diaryId);
        contextRepository.deleteAllByDiaryId(diaryId);

        Diary refreshedDiary = diaryRepository.findByIdForUpdate(diaryId)
                .orElseThrow(() -> new BaseException(ErrorCode.DIARY_NOT_FOUND));
        refreshedDiary.delete();

        eventPublisher.publishEvent(
                DiaryAudioCleanupEvent.of(answerAudioKeys, ttsAudioKeys)
        );
    }
}
