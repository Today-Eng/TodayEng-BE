package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.response.DiaryMonthlyListResponse;
import com.example.todayEng.domain.diary.dto.response.DiaryDetailResponse;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.diary.repository.DiaryAnswerRepository;
import com.example.todayEng.domain.diary.repository.DiaryQuestionRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryQueryService {

    private static final ZoneId SERVICE_ZONE_ID =
            ZoneId.of("Asia/Seoul");

    private final DiaryRepository diaryRepository;
    private final DiaryQuestionRepository diaryQuestionRepository;
    private final DiaryAnswerRepository diaryAnswerRepository;

    public DiaryMonthlyListResponse getMonthlyDiaries(
            Long userId,
            Integer year,
            Integer month
    ) {
        YearMonth targetYearMonth =
                resolveYearMonth(year, month);

        LocalDate startDate =
                targetYearMonth.atDay(1);

        LocalDate endDate =
                targetYearMonth.atEndOfMonth();

        List<Diary> diaries =
                diaryRepository
                        .findAllByUserIdAndStatusAndDiaryDateBetweenOrderByDiaryDateDesc(
                                userId,
                                DiaryStatus.COMPLETED,
                                startDate,
                                endDate
                        );

        if (diaries.isEmpty()) {
            return DiaryMonthlyListResponse.empty(
                    targetYearMonth.getYear(),
                    targetYearMonth.getMonthValue()
            );
        }

        List<Long> diaryIds =
                diaries.stream()
                        .map(Diary::getId)
                        .toList();

        List<DiaryQuestion> mainQuestions =
                diaryQuestionRepository
                        .findAllByDiaryIdInAndQuestionTypeOrderByDiaryIdAscQuestionOrderAsc(
                                diaryIds,
                                QuestionType.MAIN
                        );

        Map<Long, List<DiaryQuestion>> questionsByDiaryId =
                groupQuestionsByDiaryId(mainQuestions);

        Map<Long, DiaryQuestion> representativeQuestionsByDiaryId =
                findRepresentativeQuestions(
                        questionsByDiaryId
                );

        Map<Long, DiaryAnswer> answersByQuestionId =
                findAnswersByQuestionId(
                        representativeQuestionsByDiaryId
                );

        List<DiaryMonthlyListResponse.DiarySummary> summaries =
                diaries.stream()
                        .map(diary -> toDiarySummary(
                                diary,
                                questionsByDiaryId,
                                representativeQuestionsByDiaryId,
                                answersByQuestionId
                        ))
                        .toList();

        return DiaryMonthlyListResponse.of(
                targetYearMonth.getYear(),
                targetYearMonth.getMonthValue(),
                summaries
        );
    }

    public DiaryDetailResponse getDiaryDetail(
            Long userId,
            Long diaryId
    ) {
        Diary diary = diaryRepository
                .findByIdAndUserIdAndStatus(
                        diaryId,
                        userId,
                        DiaryStatus.COMPLETED
                )
                .orElseThrow(() -> new BaseException(
                        ErrorCode.DIARY_NOT_FOUND
                ));

        List<DiaryQuestion> questions =
                diaryQuestionRepository
                        .findAllByDiaryIdOrderByQuestionOrderAscIdAsc(
                                diaryId
                        );

        List<Long> questionIds = questions.stream()
                .map(DiaryQuestion::getId)
                .toList();

        Map<Long, DiaryAnswer> answersByQuestionId =
                findAllAnswersByQuestionId(questionIds);

        List<String> keywords = questions.stream()
                .filter(question ->
                        question.getQuestionType() == QuestionType.MAIN
                )
                .map(DiaryQuestion::getKeyword)
                .filter(Objects::nonNull)
                .filter(keyword -> !keyword.isBlank())
                .toList();

        List<DiaryDetailResponse.QuestionAnswer> qaList =
                questions.stream()
                        .map(question -> toQuestionAnswer(
                                question,
                                answersByQuestionId.get(
                                        question.getId()
                                )
                        ))
                        .toList();

        return DiaryDetailResponse.of(
                diary.getId(),
                diary.getDiaryDate(),
                keywords,
                qaList,
                diary.getMemo()
        );
    }

    private YearMonth resolveYearMonth(
            Integer year,
            Integer month
    ) {
        LocalDate today =
                LocalDate.now(SERVICE_ZONE_ID);

        int targetYear =
                year != null
                        ? year
                        : today.getYear();

        int targetMonth =
                month != null
                        ? month
                        : today.getMonthValue();

        try {
            return YearMonth.of(
                    targetYear,
                    targetMonth
            );
        } catch (DateTimeException exception) {
            throw new BaseException(
                    ErrorCode.INVALID_DIARY_YEAR_MONTH
            );
        }
    }

    private Map<Long, List<DiaryQuestion>>
    groupQuestionsByDiaryId(
            List<DiaryQuestion> questions
    ) {
        return questions.stream()
                .collect(Collectors.groupingBy(
                        question ->
                                question.getDiary()
                                        .getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<Long, DiaryQuestion>
    findRepresentativeQuestions(
            Map<Long, List<DiaryQuestion>> questionsByDiaryId
    ) {
        return questionsByDiaryId.entrySet()
                .stream()
                .filter(entry ->
                        !entry.getValue().isEmpty()
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry ->
                                entry.getValue().get(0)
                ));
    }

    private Map<Long, DiaryAnswer> findAnswersByQuestionId(
            Map<Long, DiaryQuestion> representativeQuestionsByDiaryId
    ) {
        List<Long> representativeQuestionIds =
                representativeQuestionsByDiaryId
                        .values()
                        .stream()
                        .map(DiaryQuestion::getId)
                        .toList();

        return findAllAnswersByQuestionId(
                representativeQuestionIds
        );
    }

    private DiaryMonthlyListResponse.DiarySummary
    toDiarySummary(
            Diary diary,
            Map<Long, List<DiaryQuestion>> questionsByDiaryId,
            Map<Long, DiaryQuestion> representativeQuestionsByDiaryId,
            Map<Long, DiaryAnswer> answersByQuestionId
    ) {
        List<DiaryQuestion> mainQuestions =
                questionsByDiaryId.getOrDefault(
                        diary.getId(),
                        List.of()
                );

        List<String> keywords =
                mainQuestions.stream()
                        .map(DiaryQuestion::getKeyword)
                        .filter(Objects::nonNull)
                        .filter(keyword ->
                                !keyword.isBlank()
                        )
                        .toList();

        DiaryQuestion representativeQuestion =
                representativeQuestionsByDiaryId.get(
                        diary.getId()
                );

        String questionText =
                representativeQuestion == null
                        ? null
                        : representativeQuestion.getQuestionText();

        DiaryAnswer representativeAnswer =
                representativeQuestion == null
                        ? null
                        : answersByQuestionId.get(
                        representativeQuestion.getId()
                );

        String correctedText =
                representativeAnswer == null
                        ? null
                        : representativeAnswer.getCorrectedText();

        return DiaryMonthlyListResponse.DiarySummary.of(
                diary.getId(),
                diary.getDiaryDate(),
                keywords,
                questionText,
                correctedText
        );
    }

    private Map<Long, DiaryAnswer> findAllAnswersByQuestionId(
            List<Long> questionIds
    ) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }

        return diaryAnswerRepository
                .findAllByQuestionIdIn(questionIds)
                .stream()
                .collect(Collectors.toMap(
                        answer -> answer.getQuestion().getId(),
                        Function.identity()
                ));
    }

    private DiaryDetailResponse.QuestionAnswer toQuestionAnswer(
            DiaryQuestion question,
            DiaryAnswer answer
    ) {
        DiaryDetailResponse.Answer answerResponse =
                answer == null
                        ? null
                        : DiaryDetailResponse.Answer.of(
                        answer.getOriginalText(),
                        answer.getCorrectedText(),
                        answer.getCorrectionReason(),
                        answer.getAlternativeExpression()
                );

        return DiaryDetailResponse.QuestionAnswer.of(
                question.getId(),
                question.getQuestionOrder(),
                question.getQuestionType(),
                question.getQuestionText(),
                question.getKoreanTranslation(),
                question.getKeyword(),
                answerResponse
        );
    }
}