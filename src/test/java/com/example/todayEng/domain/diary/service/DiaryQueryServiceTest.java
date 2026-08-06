package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.todayEng.domain.diary.dto.response.DiaryDetailResponse;
import com.example.todayEng.domain.diary.dto.response.DiaryMonthlyListResponse;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.entity.enums.QuestionGenerationType;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.diary.repository.DiaryAnswerRepository;
import com.example.todayEng.domain.diary.repository.DiaryQuestionRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DiaryQueryServiceTest {

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private DiaryQuestionRepository diaryQuestionRepository;

    @Mock
    private DiaryAnswerRepository diaryAnswerRepository;

    @InjectMocks
    private DiaryQueryService diaryQueryService;

    private final Long userId = 1L;
    private final User user = User.create();

    @Test
    @DisplayName("선택한 월의 완료된 회고록을 최신순으로 조회한다")
    void getMonthlyDiaries_success() {
        Diary recentDiary = createCompletedDiary(
                1L,
                LocalDate.of(2026, 7, 26)
        );

        Diary oldDiary = createCompletedDiary(
                2L,
                LocalDate.of(2026, 7, 20)
        );

        DiaryQuestion recentMainQuestion1 = createMainQuestion(
                11L,
                recentDiary,
                1,
                "How was the weather today?",
                "오늘 날씨는 어땠나요?",
                "weather"
        );

        DiaryQuestion recentMainQuestion2 = createMainQuestion(
                12L,
                recentDiary,
                2,
                "Did you use an umbrella?",
                "우산을 사용했나요?",
                "umbrella"
        );

        DiaryQuestion oldMainQuestion1 = createMainQuestion(
                21L,
                oldDiary,
                1,
                "What did you do today?",
                "오늘 무엇을 했나요?",
                "museum"
        );

        DiaryQuestion oldMainQuestion2 = createMainQuestion(
                22L,
                oldDiary,
                2,
                "How was your English study?",
                "영어 공부는 어땠나요?",
                "english study"
        );

        DiaryAnswer recentAnswer = createSucceededAnswer(
                101L,
                recentMainQuestion1,
                "It cloudy today.",
                "It was cloudy today.",
                null
        );

        DiaryAnswer oldAnswer = createSucceededAnswer(
                102L,
                oldMainQuestion1,
                "I visit museum.",
                "I visited a museum.",
                null
        );

        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 31);

        given(
                diaryRepository
                        .findAllByUserIdAndStatusAndDiaryDateBetweenOrderByDiaryDateDesc(
                                userId,
                                DiaryStatus.COMPLETED,
                                startDate,
                                endDate
                        )
        ).willReturn(List.of(
                recentDiary,
                oldDiary
        ));

        given(
                diaryQuestionRepository
                        .findAllByDiaryIdInAndQuestionTypeOrderByDiaryIdAscQuestionOrderAsc(
                                List.of(1L, 2L),
                                QuestionType.MAIN
                        )
        ).willReturn(List.of(
                recentMainQuestion1,
                recentMainQuestion2,
                oldMainQuestion1,
                oldMainQuestion2
        ));

        given(
                diaryAnswerRepository.findAllByQuestionIdIn(
                        argThat(questionIds ->
                                questionIds.size() == 2
                                        && questionIds.containsAll(
                                        List.of(11L, 21L)
                                )
                        )
                )
        ).willReturn(List.of(
                recentAnswer,
                oldAnswer
        ));

        DiaryMonthlyListResponse response =
                diaryQueryService.getMonthlyDiaries(
                        userId,
                        2026,
                        7
                );

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(7);
        assertThat(response.diaries()).hasSize(2);

        DiaryMonthlyListResponse.DiarySummary first =
                response.diaries().get(0);

        assertThat(first.diaryId()).isEqualTo(1L);
        assertThat(first.diaryDate())
                .isEqualTo(LocalDate.of(2026, 7, 26));
        assertThat(first.dayOfWeek().name())
                .isEqualTo("SUNDAY");
        assertThat(first.keywords())
                .containsExactly("weather", "umbrella");
        assertThat(first.questionText())
                .isEqualTo("How was the weather today?");
        assertThat(first.correctedText())
                .isEqualTo("It was cloudy today.");

        DiaryMonthlyListResponse.DiarySummary second =
                response.diaries().get(1);

        assertThat(second.diaryId()).isEqualTo(2L);
        assertThat(second.diaryDate())
                .isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(second.keywords())
                .containsExactly("museum", "english study");
        assertThat(second.questionText())
                .isEqualTo("What did you do today?");
        assertThat(second.correctedText())
                .isEqualTo("I visited a museum.");
    }

    @Test
    @DisplayName("해당 월에 완료된 회고록이 없으면 빈 목록을 반환한다")
    void getMonthlyDiaries_empty() {
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 31);

        given(
                diaryRepository
                        .findAllByUserIdAndStatusAndDiaryDateBetweenOrderByDiaryDateDesc(
                                userId,
                                DiaryStatus.COMPLETED,
                                startDate,
                                endDate
                        )
        ).willReturn(List.of());

        DiaryMonthlyListResponse response =
                diaryQueryService.getMonthlyDiaries(
                        userId,
                        2026,
                        7
                );

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(7);
        assertThat(response.diaries()).isEmpty();

        verifyNoInteractions(
                diaryQuestionRepository,
                diaryAnswerRepository
        );
    }

    @Test
    @DisplayName("대표 답변의 교정문이 비어 있으면 원문으로 대체한다")
    void getMonthlyDiaries_representativeCorrectedTextFallbackToOriginal() {
        Diary diary = createCompletedDiary(
                1L,
                LocalDate.of(2026, 7, 26)
        );

        DiaryQuestion mainQuestion = createMainQuestion(
                11L,
                diary,
                1,
                "How was the weather today?",
                "오늘 날씨는 어땠나요?",
                "weather"
        );

        DiaryAnswer answer = createSucceededAnswer(
                101L,
                mainQuestion,
                "It cloudy today.",
                null,
                null
        );

        given(
                diaryRepository
                        .findAllByUserIdAndStatusAndDiaryDateBetweenOrderByDiaryDateDesc(
                                userId,
                                DiaryStatus.COMPLETED,
                                LocalDate.of(2026, 7, 1),
                                LocalDate.of(2026, 7, 31)
                        )
        ).willReturn(List.of(diary));

        given(
                diaryQuestionRepository
                        .findAllByDiaryIdInAndQuestionTypeOrderByDiaryIdAscQuestionOrderAsc(
                                List.of(1L),
                                QuestionType.MAIN
                        )
        ).willReturn(List.of(mainQuestion));

        given(
                diaryAnswerRepository.findAllByQuestionIdIn(
                        List.of(11L)
                )
        ).willReturn(List.of(answer));

        DiaryMonthlyListResponse response =
                diaryQueryService.getMonthlyDiaries(
                        userId,
                        2026,
                        7
                );

        assertThat(response.diaries()).hasSize(1);
        assertThat(response.diaries().get(0).correctedText())
                .isEqualTo("It cloudy today.");
    }

    @Test
    @DisplayName("조회 월이 1에서 12 사이가 아니면 예외가 발생한다")
    void getMonthlyDiaries_invalidMonth_throws() {
        assertThatThrownBy(() ->
                diaryQueryService.getMonthlyDiaries(
                        userId,
                        2026,
                        13
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(
                        ErrorCode.INVALID_DIARY_YEAR_MONTH
                );

        verifyNoInteractions(
                diaryRepository,
                diaryQuestionRepository,
                diaryAnswerRepository
        );
    }

    @Test
    @DisplayName("완료된 회고록의 질문과 답변 및 메모를 상세 조회한다")
    void getDiaryDetail_success() {
        Diary diary = createCompletedDiary(
                1L,
                LocalDate.of(2026, 7, 10)
        );

        diary.updateMemo(
                "오늘은 친구들을 만나 기분이 정말 좋았다."
        );

        DiaryQuestion mainQuestion = createMainQuestion(
                11L,
                diary,
                1,
                "How did you feel?",
                "오늘 기분이 어땠나요?",
                "emotion"
        );

        DiaryQuestion followUpQuestion = createFollowUpQuestion(
                12L,
                diary,
                mainQuestion,
                "Why did you feel that way?",
                "왜 그렇게 느꼈나요?"
        );

        JsonNode mainAlternatives =
                JsonNodeFactory.instance.arrayNode()
                        .add("I felt really happy.")
                        .add("I was in a great mood.");

        JsonNode followUpAlternatives =
                JsonNodeFactory.instance.arrayNode()
                        .add(
                                "I felt that way because I spent time with my friends."
                        );

        DiaryAnswer mainAnswer = createSucceededAnswer(
                101L,
                mainQuestion,
                "I was very happy.",
                "I felt very happy.",
                mainAlternatives
        );

        DiaryAnswer followUpAnswer = createSucceededAnswer(
                102L,
                followUpQuestion,
                "Because I met my friends.",
                "Because I met my friends after work.",
                followUpAlternatives
        );

        given(
                diaryRepository.findByIdAndUserIdAndStatus(
                        1L,
                        userId,
                        DiaryStatus.COMPLETED
                )
        ).willReturn(Optional.of(diary));

        given(
                diaryQuestionRepository
                        .findAllByDiaryIdInReflectionOrder(
                                1L
                        )
        ).willReturn(List.of(
                mainQuestion,
                followUpQuestion
        ));

        given(
                diaryAnswerRepository.findAllByQuestionIdIn(
                        List.of(11L, 12L)
                )
        ).willReturn(List.of(
                mainAnswer,
                followUpAnswer
        ));

        DiaryDetailResponse response =
                diaryQueryService.getDiaryDetail(
                        userId,
                        1L
                );

        assertThat(response.diaryId()).isEqualTo(1L);
        assertThat(response.diaryDate())
                .isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(response.dayOfWeek().name())
                .isEqualTo("FRIDAY");
        assertThat(response.keywords())
                .containsExactly("emotion");
        assertThat(response.qaList()).hasSize(2);
        assertThat(response.memo())
                .isEqualTo(
                        "오늘은 친구들을 만나 기분이 정말 좋았다."
                );

        DiaryDetailResponse.QuestionAnswer main =
                response.qaList().get(0);

        assertThat(main.questionId()).isEqualTo(11L);
        assertThat(main.questionOrder()).isEqualTo(1);
        assertThat(main.questionType())
                .isEqualTo(QuestionType.MAIN);
        assertThat(main.questionText())
                .isEqualTo("How did you feel?");
        assertThat(main.questionKoreanTranslation())
                .isEqualTo("오늘 기분이 어땠나요?");
        assertThat(main.keyword()).isEqualTo("emotion");
        assertThat(main.answer()).isNotNull();
        assertThat(main.answer().originalText())
                .isEqualTo("I was very happy.");
        assertThat(main.answer().correctedText())
                .isEqualTo("I felt very happy.");
        assertThat(main.answer().alternativeExpression())
                .isEqualTo(mainAlternatives);

        DiaryDetailResponse.QuestionAnswer followUp =
                response.qaList().get(1);

        assertThat(followUp.questionId()).isEqualTo(12L);
        assertThat(followUp.questionOrder()).isEqualTo(2);
        assertThat(followUp.questionType())
                .isEqualTo(QuestionType.FOLLOW_UP);
        assertThat(followUp.keyword()).isNull();
        assertThat(followUp.answer()).isNotNull();
        assertThat(followUp.answer().originalText())
                .isEqualTo(
                        "Because I met my friends."
                );
        assertThat(followUp.answer().correctedText())
                .isEqualTo(
                        "Because I met my friends after work."
                );
        assertThat(followUp.answer().alternativeExpression())
                .isEqualTo(followUpAlternatives);
    }

    @Test
    @DisplayName("메모가 없는 완료 회고도 정상적으로 상세 조회한다")
    void getDiaryDetail_withoutMemo_success() {
        Diary diary = createCompletedDiary(
                1L,
                LocalDate.of(2026, 7, 10)
        );

        DiaryQuestion mainQuestion = createMainQuestion(
                11L,
                diary,
                1,
                "What did you do today?",
                "오늘 무엇을 했나요?",
                "daily"
        );

        DiaryAnswer answer = createSucceededAnswer(
                101L,
                mainQuestion,
                "I studied English today.",
                "I studied English today.",
                null
        );

        given(
                diaryRepository.findByIdAndUserIdAndStatus(
                        1L,
                        userId,
                        DiaryStatus.COMPLETED
                )
        ).willReturn(Optional.of(diary));

        given(
                diaryQuestionRepository
                        .findAllByDiaryIdInReflectionOrder(
                                1L
                        )
        ).willReturn(List.of(mainQuestion));

        given(
                diaryAnswerRepository.findAllByQuestionIdIn(
                        List.of(11L)
                )
        ).willReturn(List.of(answer));

        DiaryDetailResponse response =
                diaryQueryService.getDiaryDetail(
                        userId,
                        1L
                );

        assertThat(response.memo()).isNull();
        assertThat(response.keywords())
                .containsExactly("daily");
        assertThat(response.qaList()).hasSize(1);

        DiaryDetailResponse.QuestionAnswer questionAnswer =
                response.qaList().get(0);

        assertThat(questionAnswer.answer()).isNotNull();
        assertThat(questionAnswer.answer().originalText())
                .isEqualTo("I studied English today.");
        assertThat(questionAnswer.answer().correctedText())
                .isEqualTo("I studied English today.");
    }

    @Test
    @DisplayName("존재하지 않거나 다른 사용자의 회고록이면 예외가 발생한다")
    void getDiaryDetail_notFound_throws() {
        given(
                diaryRepository.findByIdAndUserIdAndStatus(
                        999L,
                        userId,
                        DiaryStatus.COMPLETED
                )
        ).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                diaryQueryService.getDiaryDetail(
                        userId,
                        999L
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.DIARY_NOT_FOUND);

        verifyNoInteractions(
                diaryQuestionRepository,
                diaryAnswerRepository
        );
    }

    private Diary createCompletedDiary(
            Long diaryId,
            LocalDate diaryDate
    ) {
        Diary diary = Diary.create(
                user,
                diaryDate
        );

        diary.complete();

        ReflectionTestUtils.setField(
                diary,
                "id",
                diaryId
        );

        return diary;
    }

    private DiaryQuestion createMainQuestion(
            Long questionId,
            Diary diary,
            int questionOrder,
            String questionText,
            String koreanTranslation,
            String keyword
    ) {
        DiaryQuestion question =
                DiaryQuestion.createMainQuestion(
                        diary,
                        questionOrder,
                        questionText,
                        QuestionGenerationType.AI,
                        koreanTranslation,
                        keyword,
                        null
                );

        ReflectionTestUtils.setField(
                question,
                "id",
                questionId
        );

        return question;
    }

    private DiaryQuestion createFollowUpQuestion(
            Long questionId,
            Diary diary,
            DiaryQuestion parentQuestion,
            String questionText,
            String koreanTranslation
    ) {
        DiaryQuestion question =
                DiaryQuestion.createFollowUpQuestion(
                        diary,
                        parentQuestion,
                        questionText,
                        koreanTranslation
                );

        ReflectionTestUtils.setField(
                question,
                "id",
                questionId
        );

        return question;
    }

    private DiaryAnswer createSucceededAnswer(
            Long answerId,
            DiaryQuestion question,
            String originalText,
            String correctedText,
            JsonNode alternativeExpression
    ) {
        DiaryAnswer answer = DiaryAnswer.create(
                question,
                originalText
        );

        answer.completeCorrection(
                correctedText,
                "테스트 교정 이유",
                alternativeExpression
        );

        ReflectionTestUtils.setField(
                answer,
                "id",
                answerId
        );

        return answer;
    }
}