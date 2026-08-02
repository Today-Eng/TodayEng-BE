package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionLlmResponse;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.entity.enums.ReflectionQuestionGenerationStatus;
import com.example.todayEng.domain.diary.repository.DiaryContextRepository;
import com.example.todayEng.domain.diary.repository.DiaryQuestionRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.InterestTag;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.UserInterest;
import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.example.todayEng.domain.user.entity.enums.InterestTagName;
import com.example.todayEng.domain.user.repository.UserInterestRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReflectionQuestionPersistenceServiceTest {

    @Mock private DiaryRepository diaryRepository;
    @Mock private DiaryContextRepository contextRepository;
    @Mock private DiaryQuestionRepository questionRepository;
    @Mock private UserInterestRepository userInterestRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ReflectionQuestionPersistenceService service;
    private User user;
    private Diary diary;
    private DiaryContext context;

    @BeforeEach
    void setUp() {
        service = new ReflectionQuestionPersistenceService(
                diaryRepository,
                contextRepository,
                questionRepository,
                userInterestRepository,
                objectMapper
        );
        user = User.create();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.updateEnglishLevel(EnglishLevel.INTERMEDIATE);
        diary = Diary.create(user, LocalDate.now());
        ReflectionTestUtils.setField(diary, "id", 10L);
        context = DiaryContext.success(
                diary,
                DiaryContextType.MEMO,
                objectMapper.createObjectNode().put("memo", "Had lunch")
        );
        ReflectionTestUtils.setField(context, "id", 100L);
    }

    @Test
    void preparesSnapshotAfterAtomicClaim() {
        InterestTag tag = InterestTag.create(InterestTagName.MUSIC);
        UserInterest interest = UserInterest.create(user, tag);
        given(diaryRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(diary));
        given(diaryRepository.claimQuestionGeneration(10L, 1L))
                .willReturn(1);
        given(contextRepository.findAllByDiaryIdAndSuccessTrueOrderById(10L))
                .willReturn(List.of(context));
        given(userInterestRepository.findAllByUserIdOrderByInterestTagId(1L))
                .willReturn(List.of(interest));

        var result = service.prepare(1L, 10L);

        assertThat(result.englishLevel()).isEqualTo(EnglishLevel.INTERMEDIATE);
        assertThat(result.interests()).containsExactly("MUSIC");
        assertThat(result.contexts()).hasSize(1);
        assertThat(result.contexts().get(0).contextId()).isEqualTo(100L);
    }

    @Test
    void rejectsDuplicateGenerationBeforeLlmCall() {
        given(diaryRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(diary));
        given(diaryRepository.claimQuestionGeneration(10L, 1L))
                .willReturn(0);

        assertThatThrownBy(() -> service.prepare(1L, 10L))
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.REFLECTION_QUESTIONS_ALREADY_GENERATED);
    }

    @Test
    void savesThreeQuestionsWithContextAndSnapshots() {
        var command = new com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand(
                1L,
                10L,
                EnglishLevel.INTERMEDIATE,
                List.of("MUSIC"),
                List.of(new com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand.ContextInput(
                        100L,
                        DiaryContextType.MEMO,
                        context.getContextData()
                ))
        );
        var response = new ReflectionQuestionLlmResponse(List.of(
                question(3, 100L),
                question(1, 100L),
                question(2, 100L)
        ));
        given(diaryRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(diary));
        given(contextRepository.findAllByDiaryIdAndIdIn(10L, List.of(100L)))
                .willReturn(List.of(context));
        given(questionRepository.saveAllAndFlush(anyList()))
                .willAnswer(invocation -> {
                    List<DiaryQuestion> questions = invocation.getArgument(0);
                    for (int index = 0; index < questions.size(); index++) {
                        ReflectionTestUtils.setField(
                                questions.get(index),
                                "id",
                                (long) index + 1
                        );
                    }
                    return questions;
                });

        var result = service.saveQuestions(command, response);

        assertThat(result.questions()).extracting(
                com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse.Question::order
        ).containsExactly(1, 2, 3);
        assertThat(result.questions()).extracting(
                com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse.Question::contextId
        ).containsOnly(100L);
        assertThat(diary.getQuestionGenerationStatus())
                .isEqualTo(ReflectionQuestionGenerationStatus.COMPLETED);
    }

    @Test
    void rejectsContextIdThatWasNotProvidedToLlm() {
        var command = new com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand(
                1L,
                10L,
                EnglishLevel.BEGINNER,
                List.of(),
                List.of(new com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand.ContextInput(
                        100L,
                        DiaryContextType.MEMO,
                        context.getContextData()
                ))
        );
        var response = new ReflectionQuestionLlmResponse(List.of(
                question(1, 999L),
                question(2, 100L),
                question(3, 100L)
        ));

        assertThatThrownBy(() -> service.saveQuestions(command, response))
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.INVALID_QUESTION_CONTEXT);
    }

    private ReflectionQuestionLlmResponse.GeneratedQuestion question(
            int order,
            long contextId
    ) {
        return new ReflectionQuestionLlmResponse.GeneratedQuestion(
                order,
                "How was your lunch?",
                "점심은 어땠나요?",
                "lunch",
                contextId
        );
    }
}
