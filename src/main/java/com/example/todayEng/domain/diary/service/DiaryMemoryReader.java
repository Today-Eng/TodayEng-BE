package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand.DiaryInput;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand.ReflectionInput;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryAnswerRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiaryMemoryReader {

    private static final int MEMORY_DAYS = 30;
    private static final int MAX_DIARY_COUNT = 10;
    private static final int MAX_MEMO_LENGTH = 2000;
    private static final int MAX_QUESTION_LENGTH = 1000;
    private static final int MAX_ANSWER_LENGTH = 4000;

    private final DiaryRepository diaryRepository;
    private final DiaryAnswerRepository answerRepository;

    public DiaryMemoryReader(
            DiaryRepository diaryRepository,
            DiaryAnswerRepository answerRepository
    ) {
        this.diaryRepository = diaryRepository;
        this.answerRepository = answerRepository;
    }

    @Transactional(readOnly = true)
    public Optional<DiaryMemoryAnalysisCommand> prepare(
            Long userId,
            Long currentDiaryId
    ) {
        Diary currentDiary = diaryRepository
                .findByIdAndUserId(currentDiaryId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.ACCESS_DENIED));
        if (currentDiary.getStatus() != DiaryStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.DIARY_NOT_IN_PROGRESS);
        }

        List<Diary> diaries = diaryRepository.findRecentCompletedForMemory(
                userId,
                currentDiaryId,
                currentDiary.getDiaryDate().minusDays(MEMORY_DAYS),
                currentDiary.getDiaryDate(),
                PageRequest.of(0, MAX_DIARY_COUNT)
        );
        if (diaries.isEmpty()) {
            return Optional.empty();
        }

        List<Long> diaryIds = diaries.stream().map(Diary::getId).toList();
        Map<Long, List<DiaryAnswer>> answersByDiaryId = answerRepository
                .findAllForMemoryAnalysis(diaryIds)
                .stream()
                .collect(Collectors.groupingBy(
                        answer -> answer.getQuestion().getDiary().getId()
                ));

        List<DiaryInput> inputs = diaries.stream()
                .map(diary -> toInput(
                        diary,
                        answersByDiaryId.getOrDefault(diary.getId(), List.of())
                ))
                .filter(this::hasContent)
                .toList();
        if (inputs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DiaryMemoryAnalysisCommand(
                currentDiaryId,
                inputs
        ));
    }

    private DiaryInput toInput(Diary diary, List<DiaryAnswer> answers) {
        List<ReflectionInput> reflections = answers.stream()
                .map(answer -> new ReflectionInput(
                        truncate(answer.getQuestion().getQuestionText(),
                                MAX_QUESTION_LENGTH),
                        truncate(preferredAnswer(answer), MAX_ANSWER_LENGTH)
                ))
                .filter(reflection -> !isBlank(reflection.question())
                        && !isBlank(reflection.answer()))
                .toList();
        return new DiaryInput(
                diary.getId(),
                diary.getDiaryDate(),
                truncate(diary.getMemo(), MAX_MEMO_LENGTH),
                reflections
        );
    }

    private String preferredAnswer(DiaryAnswer answer) {
        return isBlank(answer.getCorrectedText())
                ? answer.getOriginalText()
                : answer.getCorrectedText();
    }

    private boolean hasContent(DiaryInput input) {
        return !isBlank(input.memo()) || !input.reflections().isEmpty();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
