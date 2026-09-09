package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.diary.entity.*;
import com.example.todayEng.domain.diary.entity.enums.QuestionGenerationType;
import com.example.todayEng.domain.diary.repository.*;
import com.example.todayEng.domain.diary.sse.DiarySseEmitterManager;
import com.example.todayEng.domain.diary.storage.AudioFileStorage;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({DiaryAnswerQueryService.class, DiaryQuestionQueryService.class,
        AnswerPersistenceService.class, DiarySubscriptionService.class})
class DiaryOwnershipIntegrationTest {
    @Autowired UserRepository userRepository;
    @Autowired DiaryRepository diaryRepository;
    @Autowired DiaryQuestionRepository questionRepository;
    @Autowired DiaryAnswerRepository answerRepository;
    @Autowired DiaryAnswerQueryService answerQueryService;
    @Autowired DiaryQuestionQueryService questionQueryService;
    @Autowired AnswerPersistenceService answerPersistenceService;
    @Autowired DiarySubscriptionService subscriptionService;
    @Autowired EntityManager entityManager;
    @MockBean AudioFileStorage audioFileStorage;
    @MockBean DiarySseEmitterManager emitterManager;

    private Long ownerId, otherUserId, ownerDiaryId, ownerQuestionId, ownerAnswerId;
    private Long otherDiaryId, otherQuestionId, otherAnswerId;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.create());
        User other = userRepository.save(User.create());
        Diary ownerDiary = diaryRepository.save(Diary.create(owner, LocalDate.of(2026, 9, 8)));
        Diary otherDiary = diaryRepository.save(Diary.create(other, LocalDate.of(2026, 9, 8)));
        DiaryQuestion ownerQuestion = questionRepository.save(mainQuestion(ownerDiary));
        DiaryQuestion otherQuestion = questionRepository.save(mainQuestion(otherDiary));
        DiaryAnswer ownerAnswer = answerRepository.save(DiaryAnswer.create(ownerQuestion, "owner"));
        DiaryAnswer otherAnswer = answerRepository.save(DiaryAnswer.create(otherQuestion, "other"));
        entityManager.flush();
        ownerId = owner.getId();
        otherUserId = other.getId();
        ownerDiaryId = ownerDiary.getId();
        ownerQuestionId = ownerQuestion.getId();
        ownerAnswerId = ownerAnswer.getId();
        otherDiaryId = otherDiary.getId();
        otherQuestionId = otherQuestion.getId();
        otherAnswerId = otherAnswer.getId();
        entityManager.clear();
    }

    @Test
    void otherUserCannotReadDiaryQuestionsOrAnswers() {
        assertError(() -> questionQueryService.getQuestions(otherUserId, ownerDiaryId),
                ErrorCode.ACCESS_DENIED);
        assertError(() -> answerQueryService.getAnswers(otherUserId, ownerDiaryId),
                ErrorCode.ACCESS_DENIED);
        assertError(() -> answerQueryService.getAnswer(otherUserId, ownerDiaryId, ownerAnswerId),
                ErrorCode.ACCESS_DENIED);
    }

    @Test
    void questionMustBelongToAuthenticatedUsersDiary() {
        assertError(() -> answerPersistenceService.validateAnswerable(
                ownerId, ownerDiaryId, otherQuestionId), ErrorCode.QUESTION_NOT_FOUND);
        assertError(() -> answerPersistenceService.validateAnswerable(
                ownerId, otherDiaryId, otherQuestionId), ErrorCode.QUESTION_NOT_FOUND);
    }

    @Test
    void answerMustBelongToRequestedDiaryAndAuthenticatedUser() {
        assertError(() -> answerQueryService.getAnswer(ownerId, ownerDiaryId, otherAnswerId),
                ErrorCode.ANSWER_NOT_FOUND);
        assertError(() -> answerPersistenceService.getOwned(
                ownerId, ownerDiaryId, ownerQuestionId, otherAnswerId), ErrorCode.ANSWER_NOT_FOUND);
        assertThat(answerPersistenceService.getOwned(
                ownerId, ownerDiaryId, ownerQuestionId, ownerAnswerId).getId()).isEqualTo(ownerAnswerId);
    }

    @Test
    void otherUserCannotSubscribeToDiaryEvents() {
        assertError(() -> subscriptionService.subscribe(otherUserId, ownerDiaryId),
                ErrorCode.ACCESS_DENIED);
        verify(emitterManager, never()).subscribe(otherUserId, ownerDiaryId);
    }

    private DiaryQuestion mainQuestion(Diary diary) {
        return DiaryQuestion.createMainQuestion(
                diary, 1, "question", QuestionGenerationType.AI, "질문", "keyword", null);
    }

    private void assertError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode errorCode) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BaseException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
