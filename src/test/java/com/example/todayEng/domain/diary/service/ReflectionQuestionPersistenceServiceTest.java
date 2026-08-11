package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
                objectMapper,
                Clock.systemDefaultZone()
        );
        user = User.create();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.updateProfile("성연", null);
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
        ReflectionTestUtils.setField(diary, "questionGenerationLeaseVersion", 7L);
        given(diaryRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(diary));
        given(diaryRepository.claimQuestionGeneration(eq(10L), eq(1L), any(LocalDateTime.class)))
                .willReturn(1);
        given(contextRepository.findAllByDiaryIdAndSuccessTrueOrderById(10L))
                .willReturn(List.of(context));
        given(userInterestRepository.findAllByUserIdOrderByInterestTagId(1L))
                .willReturn(List.of(interest));

        var result = service.prepare(1L, 10L);

        assertThat(result.leaseVersion()).isEqualTo(7L);
        assertThat(result.nickname()).isEqualTo("성연");
        assertThat(result.englishLevel()).isEqualTo(EnglishLevel.INTERMEDIATE);
        assertThat(result.interests()).containsExactly("MUSIC");
        assertThat(result.contexts()).hasSize(1);
        assertThat(result.contexts().get(0).contextId()).isEqualTo(100L);
    }

    @Test
    void preparesTwoPhotoContextsAsDifferentQuestionSources() {
        DiaryContext firstPhoto = DiaryContext.success(
                diary, DiaryContextType.PHOTO, 0,
                objectMapper.createObjectNode().put("summary", "공원"));
        DiaryContext secondPhoto = DiaryContext.success(
                diary, DiaryContextType.PHOTO, 1,
                objectMapper.createObjectNode().put("summary", "카페"));
        ReflectionTestUtils.setField(firstPhoto, "id", 101L);
        ReflectionTestUtils.setField(secondPhoto, "id", 102L);
        given(diaryRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(diary));
        given(diaryRepository.claimQuestionGeneration(
                eq(10L), eq(1L), any(LocalDateTime.class))).willReturn(1);
        given(contextRepository.findAllByDiaryIdAndSuccessTrueOrderById(10L))
                .willReturn(List.of(firstPhoto, secondPhoto));
        InterestTag tag = InterestTag.create(InterestTagName.MUSIC);
        given(userInterestRepository.findAllByUserIdOrderByInterestTagId(1L))
                .willReturn(List.of(UserInterest.create(user, tag)));

        var result = service.prepare(1L, 10L);

        assertThat(result.contexts())
                .extracting(context -> context.contextId())
                .containsExactly(101L, 102L);
        assertThat(result.plan().contextQuestionCount()).isEqualTo(2);
        assertThat(result.plan().requireDistinctContexts()).isTrue();
    }

    @Test
    void usesInterestFallbackWhenDiaryContextIsAbsent() {
        InterestTag tag = InterestTag.create(InterestTagName.MUSIC);
        UserInterest interest = UserInterest.create(user, tag);
        given(diaryRepository.findByIdAndUserId(10L, 1L)).willReturn(Optional.of(diary));
        given(diaryRepository.claimQuestionGeneration(eq(10L), eq(1L), any(LocalDateTime.class))).willReturn(1);
        given(contextRepository.findAllByDiaryIdAndSuccessTrueOrderById(10L)).willReturn(List.of());
        given(userInterestRepository.findAllByUserIdOrderByInterestTagId(1L)).willReturn(List.of(interest));

        var result = service.prepare(1L, 10L);

        assertThat(result.contexts()).isEmpty();
        assertThat(result.interests()).containsExactly("MUSIC");
    }

    @Test
    void rejectsGenerationWhenBothContextAndInterestAreAbsent() {
        given(diaryRepository.findByIdAndUserId(10L, 1L)).willReturn(Optional.of(diary));
        given(diaryRepository.claimQuestionGeneration(eq(10L), eq(1L), any(LocalDateTime.class))).willReturn(1);
        given(contextRepository.findAllByDiaryIdAndSuccessTrueOrderById(10L)).willReturn(List.of());
        given(userInterestRepository.findAllByUserIdOrderByInterestTagId(1L)).willReturn(List.of());

        assertThatThrownBy(() -> service.prepare(1L, 10L))
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DIARY_CONTEXT_NOT_FOUND));
    }

    @Test
    void rejectsDuplicateGenerationBeforeLlmCall() {
        given(diaryRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(diary));
        given(diaryRepository.claimQuestionGeneration(eq(10L), eq(1L), any(LocalDateTime.class)))
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
                1L,
                "성연",
                EnglishLevel.INTERMEDIATE,
                List.of("MUSIC"),
                List.of(new com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand.ContextInput(
                        100L,
                        DiaryContextType.MEMO,
                        context.getContextData()
                ))
        );
        var response = new ReflectionQuestionLlmResponse(List.of(
                question(3, null),
                question(1, 100L),
                question(2, null)
        ));
        given(diaryRepository.finishQuestionGenerationIfOwned(
                10L, 1L, ReflectionQuestionGenerationStatus.COMPLETED, 1L))
                .willReturn(1);
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
        ).containsExactly(1, 3, 5);
        assertThat(result.questions()).extracting(
                com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse.Question::contextId
        ).containsExactly(100L, null, null);
        verify(diaryRepository).finishQuestionGenerationIfOwned(
                10L, 1L, ReflectionQuestionGenerationStatus.COMPLETED, 1L);
    }

    @Test
    void rejectsSaveWhenClaimWasReclaimedByAnotherRequest() {
        var command = new com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand(
                1L,
                10L,
                1L,
                "성연",
                EnglishLevel.INTERMEDIATE,
                List.of("MUSIC"),
                List.of(new com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand.ContextInput(
                        100L,
                        DiaryContextType.MEMO,
                        context.getContextData()
                ))
        );
        var response = new ReflectionQuestionLlmResponse(List.of(
                question(1, 100L),
                question(2, null),
                question(3, null)
        ));
        given(diaryRepository.finishQuestionGenerationIfOwned(
                10L, 1L, ReflectionQuestionGenerationStatus.COMPLETED, 1L))
                .willReturn(0);

        assertThatThrownBy(() -> service.saveQuestions(command, response))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.REFLECTION_QUESTION_CLAIM_LOST);

        verify(questionRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void rejectsContextIdThatWasNotProvidedToLlm() {
        var command = new com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand(
                1L,
                10L,
                1L,
                "성연",
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

    @Test
    void rejectsRepeatedContextWhenDistinctContextsAreAvailable() {
        var command = new com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand(
                1L,
                10L,
                1L,
                "성연",
                EnglishLevel.INTERMEDIATE,
                List.of("MUSIC"),
                List.of(contextInput(100L), contextInput(101L), contextInput(102L))
        );
        var response = new ReflectionQuestionLlmResponse(List.of(
                question(1, 100L),
                question(2, 100L),
                question(3, 101L)
        ));

        assertThatThrownBy(() -> service.saveQuestions(command, response))
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_QUESTION_CONTEXT));

        verify(questionRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void rejectsContextGroundedQuestionsBeyondAvailableContexts() {
        var command = new com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand(
                1L,
                10L,
                1L,
                "성연",
                EnglishLevel.INTERMEDIATE,
                List.of("MUSIC"),
                List.of(contextInput(100L))
        );
        var response = new ReflectionQuestionLlmResponse(List.of(
                question(1, 100L),
                question(2, 100L),
                question(3, 100L)
        ));

        assertThatThrownBy(() -> service.saveQuestions(command, response))
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_QUESTION_CONTEXT));
    }

    @Test
    void rejectsInterestQuestionWhenEveryQuestionMustBeContextGrounded() {
        var command = new com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand(
                1L,
                10L,
                1L,
                "성연",
                EnglishLevel.INTERMEDIATE,
                List.of("MUSIC"),
                List.of(contextInput(100L), contextInput(101L), contextInput(102L))
        );
        var response = new ReflectionQuestionLlmResponse(List.of(
                question(1, 100L),
                question(2, 101L),
                question(3, null)
        ));

        assertThatThrownBy(() -> service.saveQuestions(command, response))
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_QUESTION_CONTEXT));
    }

    @Test
    void allowsRepeatedContextWhenUserHasNoInterests() {
        var command = new com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand(
                1L,
                10L,
                1L,
                "성연",
                EnglishLevel.INTERMEDIATE,
                List.of(),
                List.of(contextInput(100L))
        );
        var response = new ReflectionQuestionLlmResponse(List.of(
                question(1, 100L),
                question(2, 100L),
                question(3, 100L)
        ));
        given(diaryRepository.finishQuestionGenerationIfOwned(
                10L, 1L, ReflectionQuestionGenerationStatus.COMPLETED, 1L))
                .willReturn(1);
        given(diaryRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(diary));
        given(contextRepository.findAllByDiaryIdAndIdIn(10L, List.of(100L)))
                .willReturn(List.of(context));
        given(questionRepository.saveAllAndFlush(anyList()))
                .willAnswer(invocation -> invocation.getArgument(0));

        var result = service.saveQuestions(command, response);

        assertThat(result.questions()).extracting(
                com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse.Question::contextId
        ).containsExactly(100L, 100L, 100L);
    }

    @Test
    void rejectsIgnoringSecondPhotoWhenContextReuseIsRequired() {
        var command = new com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand(
                1L,
                10L,
                1L,
                "성연",
                EnglishLevel.INTERMEDIATE,
                List.of(),
                List.of(contextInput(100L), contextInput(101L))
        );
        var response = new ReflectionQuestionLlmResponse(List.of(
                question(1, 100L),
                question(2, 100L),
                question(3, 100L)
        ));

        assertThatThrownBy(() -> service.saveQuestions(command, response))
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_QUESTION_CONTEXT));
    }

    private com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand.ContextInput contextInput(
            long contextId
    ) {
        return new com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand.ContextInput(
                contextId,
                DiaryContextType.MEMO,
                context.getContextData()
        );
    }

    private ReflectionQuestionLlmResponse.GeneratedQuestion question(
            int order,
            Long contextId
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
