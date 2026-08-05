package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.response.*;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.CorrectionStatus;
import com.example.todayEng.domain.diary.entity.enums.TtsStatus;
import com.example.todayEng.domain.diary.repository.DiaryAnswerRepository;
import com.example.todayEng.domain.diary.repository.DiaryQuestionRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.diary.storage.AudioFileStorage;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryQuestionQueryService {
    private static final int EXPECTED_QUESTION_COUNT = 6;

    private final DiaryRepository diaryRepository;
    private final DiaryQuestionRepository questionRepository;
    private final DiaryAnswerRepository answerRepository;
    private final AudioFileStorage audioFileStorage;

    public DiaryQuestionListResponse getQuestions(Long userId, Long diaryId) {
        Diary diary = ownedDiary(userId, diaryId);
        QueryData data = load(diaryId);
        return new DiaryQuestionListResponse(diaryId, diary.getStatus(), diary.getQuestionGenerationStatus(),
                data.questions().stream().map(question -> toResponse(question,
                        data.answersByQuestionId().get(question.getId()))).toList());
    }

    public NextDiaryQuestionResponse getNextQuestion(Long userId, Long diaryId) {
        ownedDiary(userId, diaryId);
        QueryData data = load(diaryId);

        for (DiaryQuestion question : data.questions()) {
            DiaryAnswer answer = data.answersByQuestionId().get(question.getId());
            if (answer == null) {
                if (question.getTtsStatus() == TtsStatus.SUCCEEDED) {
                    return new NextDiaryQuestionResponse(NextQuestionStatus.QUESTION_READY,
                            toResponse(question, null));
                }
                return new NextDiaryQuestionResponse(NextQuestionStatus.WAITING, null);
            }
            if (answer.getCorrectionStatus() != CorrectionStatus.SUCCEEDED) {
                return new NextDiaryQuestionResponse(NextQuestionStatus.WAITING, null);
            }
        }

        boolean completedFlow = data.questions().size() == EXPECTED_QUESTION_COUNT
                && data.answersByQuestionId().size() == EXPECTED_QUESTION_COUNT;
        return new NextDiaryQuestionResponse(
                completedFlow ? NextQuestionStatus.READY_TO_COMPLETE : NextQuestionStatus.WAITING,
                null
        );
    }

    private QueryData load(Long diaryId) {
        var questions = questionRepository.findAllByDiaryIdInReflectionOrder(diaryId);
        Map<Long, DiaryAnswer> answers = answerRepository.findAllByDiaryIdInReflectionOrder(diaryId).stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), Function.identity()));
        return new QueryData(questions, answers);
    }

    private DiaryQuestionResponse toResponse(DiaryQuestion question, DiaryAnswer answer) {
        String audioUrl = question.getTtsAudioKey() == null
                ? null : audioFileStorage.publicUrl(question.getTtsAudioKey());
        return DiaryQuestionResponse.from(question, answer, audioUrl);
    }

    private Diary ownedDiary(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new BaseException(ErrorCode.DIARY_NOT_FOUND));
        if (!diary.getUser().getId().equals(userId)) throw new BaseException(ErrorCode.ACCESS_DENIED);
        return diary;
    }

    private record QueryData(java.util.List<DiaryQuestion> questions,
            Map<Long, DiaryAnswer> answersByQuestionId) { }
}
