package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.example.todayEng.domain.diary.entity.*;
import com.example.todayEng.domain.diary.entity.enums.QuestionGenerationType;
import com.example.todayEng.domain.diary.repository.*;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DiaryAnswerQueryServiceTest {
    @Mock DiaryRepository diaryRepository;
    @Mock DiaryQuestionRepository questionRepository;
    @Mock DiaryAnswerRepository answerRepository;
    @InjectMocks DiaryAnswerQueryService service;
    User user; Diary diary;

    @BeforeEach void setUp() {
        user = User.create(); ReflectionTestUtils.setField(user, "id", 1L);
        diary = Diary.create(user, LocalDate.now()); ReflectionTestUtils.setField(diary, "id", 2L);
    }

    @Test void getsOwnedAnswer() {
        DiaryAnswer answer = answer(3L, 4L, 1);
        when(diaryRepository.findById(2L)).thenReturn(Optional.of(diary));
        when(answerRepository.findDetailById(3L)).thenReturn(Optional.of(answer));
        assertThat(service.getAnswer(1L, 2L, 3L).answerId()).isEqualTo(3L);
    }

    @Test void rejectsMissingDiary() {
        when(diaryRepository.findById(2L)).thenReturn(Optional.empty());
        assertError(() -> service.getAnswer(1L, 2L, 3L), ErrorCode.DIARY_NOT_FOUND);
    }

    @Test void rejectsOtherOwner() {
        when(diaryRepository.findById(2L)).thenReturn(Optional.of(diary));
        assertError(() -> service.getAnswer(99L, 2L, 3L), ErrorCode.ACCESS_DENIED);
    }

    @Test void rejectsMissingAnswer() {
        when(diaryRepository.findById(2L)).thenReturn(Optional.of(diary));
        when(answerRepository.findDetailById(3L)).thenReturn(Optional.empty());
        assertError(() -> service.getAnswer(1L, 2L, 3L), ErrorCode.ANSWER_NOT_FOUND);
    }

    @Test void rejectsAnswerFromDifferentDiary() {
        Diary other = Diary.create(user, LocalDate.now().minusDays(1)); ReflectionTestUtils.setField(other, "id", 9L);
        DiaryQuestion q = DiaryQuestion.createMainQuestion(other, 1, "q", QuestionGenerationType.AI, "k", null, null);
        DiaryAnswer a = DiaryAnswer.create(q, "a"); ReflectionTestUtils.setField(a, "id", 3L);
        when(diaryRepository.findById(2L)).thenReturn(Optional.of(diary));
        when(answerRepository.findDetailById(3L)).thenReturn(Optional.of(a));
        assertError(() -> service.getAnswer(1L, 2L, 3L), ErrorCode.ANSWER_NOT_FOUND);
    }

    @Test void listsRepositoryOrderedAnswersAndCountsPartialResults() {
        DiaryAnswer first = answer(10L, 20L, 1); DiaryAnswer second = answer(11L, 21L, 2);
        when(diaryRepository.findById(2L)).thenReturn(Optional.of(diary));
        when(answerRepository.findAllByDiaryIdInReflectionOrder(2L)).thenReturn(List.of(first, second));
        when(questionRepository.countByDiaryId(2L)).thenReturn(6L);
        var result = service.getAnswers(1L, 2L);
        assertThat(result.answeredCount()).isEqualTo(2);
        assertThat(result.expectedAnswerCount()).isEqualTo(6);
        assertThat(result.answers()).extracting(a -> a.questionOrder()).containsExactly(1, 2);
        verify(answerRepository, times(1)).findAllByDiaryIdInReflectionOrder(2L);
    }

    private DiaryAnswer answer(Long answerId, Long questionId, int order) {
        DiaryQuestion q = DiaryQuestion.createMainQuestion(diary, order, "q", QuestionGenerationType.AI, "k", null, null);
        ReflectionTestUtils.setField(q, "id", questionId);
        DiaryAnswer a = DiaryAnswer.create(q, "a"); ReflectionTestUtils.setField(a, "id", answerId); return a;
    }
    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BaseException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }
}
