package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.event.DiaryAudioCleanupEvent;
import com.example.todayEng.domain.diary.repository.DiaryAnswerRepository;
import com.example.todayEng.domain.diary.repository.DiaryContextRepository;
import com.example.todayEng.domain.diary.repository.DiaryContextSourceRepository;
import com.example.todayEng.domain.diary.repository.DiaryQuestionRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DiaryDeletionServiceTest {

    @Mock private DiaryRepository diaryRepository;
    @Mock private DiaryQuestionRepository questionRepository;
    @Mock private DiaryAnswerRepository answerRepository;
    @Mock private DiaryContextRepository contextRepository;
    @Mock private DiaryContextSourceRepository contextSourceRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private DiaryDeletionService diaryDeletionService;

    private static final Long USER_ID = 1L;
    private static final Long DIARY_ID = 10L;

    private User owner;
    private Diary completedDiary;

    @BeforeEach
    void setUp() {
        owner = User.create();
        ReflectionTestUtils.setField(owner, "id", USER_ID);

        completedDiary = Diary.create(owner, LocalDate.now().minusDays(1));
        completedDiary.complete("삭제 전 메모");
        ReflectionTestUtils.setField(completedDiary, "id", DIARY_ID);
    }

    @Test
    void completedDiaryIsMarkedDeletedAndMemoCleared() {
        given(diaryRepository.findByIdForUpdate(DIARY_ID)).willReturn(Optional.of(completedDiary));
        given(answerRepository.findAudioKeysByDiaryId(DIARY_ID)).willReturn(List.of());
        given(questionRepository.findTtsAudioKeysByDiaryId(DIARY_ID)).willReturn(List.of());

        diaryDeletionService.delete(USER_ID, DIARY_ID);

        assertThat(completedDiary.getStatus()).isEqualTo(DiaryStatus.DELETED);
        assertThat(completedDiary.getMemo()).isNull();
    }

    @Test
    void diaryRowIsRetainedAfterDeletion() {
        given(diaryRepository.findByIdForUpdate(DIARY_ID)).willReturn(Optional.of(completedDiary));
        given(answerRepository.findAudioKeysByDiaryId(DIARY_ID)).willReturn(List.of());
        given(questionRepository.findTtsAudioKeysByDiaryId(DIARY_ID)).willReturn(List.of());

        diaryDeletionService.delete(USER_ID, DIARY_ID);

        // Diary entity itself is never removed — only status transitions to DELETED
        then(diaryRepository).should(never()).delete(completedDiary);
        then(diaryRepository).should(never()).deleteById(DIARY_ID);
    }

    @Test
    void allChildDataIsDeletedInFkSafeOrder() {
        given(diaryRepository.findByIdForUpdate(DIARY_ID))
                .willReturn(Optional.of(completedDiary));
        given(answerRepository.findAudioKeysByDiaryId(DIARY_ID))
                .willReturn(List.of());
        given(questionRepository.findTtsAudioKeysByDiaryId(DIARY_ID))
                .willReturn(List.of());

        diaryDeletionService.delete(USER_ID, DIARY_ID);

        var inOrder = inOrder(
                contextSourceRepository,
                answerRepository,
                questionRepository,
                contextRepository
        );

        inOrder.verify(contextSourceRepository)
                .deleteAllBySourceDiaryId(DIARY_ID);

        inOrder.verify(contextSourceRepository)
                .deleteAllByContextDiaryId(DIARY_ID);

        inOrder.verify(answerRepository)
                .deleteAllByDiaryId(DIARY_ID);

        inOrder.verify(questionRepository)
                .deleteFollowUpsByDiaryId(DIARY_ID);

        inOrder.verify(questionRepository)
                .deleteAllByDiaryId(DIARY_ID);

        inOrder.verify(contextRepository)
                .deleteAllByDiaryId(DIARY_ID);
    }

    @Test
    void audioCleanupEventIsPublishedWithCollectedKeys() {
        List<String> answerKeys = List.of("answers/a1.ogg", "answers/a2.ogg");
        List<String> ttsKeys = List.of("tts/q1.mp3");
        given(diaryRepository.findByIdForUpdate(DIARY_ID)).willReturn(Optional.of(completedDiary));
        given(answerRepository.findAudioKeysByDiaryId(DIARY_ID)).willReturn(answerKeys);
        given(questionRepository.findTtsAudioKeysByDiaryId(DIARY_ID)).willReturn(ttsKeys);

        diaryDeletionService.delete(USER_ID, DIARY_ID);

        ArgumentCaptor<DiaryAudioCleanupEvent> captor = forClass(DiaryAudioCleanupEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        assertThat(captor.getValue().audioKeys())
                .containsExactlyInAnyOrder("answers/a1.ogg", "answers/a2.ogg", "tts/q1.mp3");
    }

    @Test
    void audioCleanupEventIsPublishedWithEmptyKeysWhenNoAudioExists() {
        given(diaryRepository.findByIdForUpdate(DIARY_ID)).willReturn(Optional.of(completedDiary));
        given(answerRepository.findAudioKeysByDiaryId(DIARY_ID)).willReturn(List.of());
        given(questionRepository.findTtsAudioKeysByDiaryId(DIARY_ID)).willReturn(List.of());

        diaryDeletionService.delete(USER_ID, DIARY_ID);

        ArgumentCaptor<DiaryAudioCleanupEvent> captor = forClass(DiaryAudioCleanupEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        assertThat(captor.getValue().audioKeys()).isEmpty();
    }

    @Test
    void otherUserCannotDeleteDiary() {
        given(diaryRepository.findByIdForUpdate(DIARY_ID)).willReturn(Optional.of(completedDiary));

        assertError(() -> diaryDeletionService.delete(2L, DIARY_ID), ErrorCode.ACCESS_DENIED);
    }

    @Test
    void inProgressDiaryCannotBeDeleted() {
        Diary inProgress = inProgressDiary();
        given(diaryRepository.findByIdForUpdate(DIARY_ID)).willReturn(Optional.of(inProgress));

        assertError(() -> diaryDeletionService.delete(USER_ID, DIARY_ID), ErrorCode.DIARY_DELETE_NOT_ALLOWED);
    }

    @Test
    void pausedDiaryCannotBeDeleted() {
        Diary paused = pausedDiary();
        given(diaryRepository.findByIdForUpdate(DIARY_ID)).willReturn(Optional.of(paused));

        assertError(() -> diaryDeletionService.delete(USER_ID, DIARY_ID), ErrorCode.DIARY_DELETE_NOT_ALLOWED);
    }

    @Test
    void alreadyDeletedDiaryCannotBeDeletedAgain() {
        Diary deleted = deletedDiary();
        given(diaryRepository.findByIdForUpdate(DIARY_ID)).willReturn(Optional.of(deleted));

        assertError(() -> diaryDeletionService.delete(USER_ID, DIARY_ID), ErrorCode.DIARY_ALREADY_DELETED);
    }

    @Test
    void notFoundThrowsDiaryNotFound() {
        given(diaryRepository.findByIdForUpdate(DIARY_ID)).willReturn(Optional.empty());

        assertError(() -> diaryDeletionService.delete(USER_ID, DIARY_ID), ErrorCode.DIARY_NOT_FOUND);
    }

    private Diary inProgressDiary() {
        Diary d = Diary.create(owner, LocalDate.now());
        ReflectionTestUtils.setField(d, "id", DIARY_ID);
        return d;
    }

    private Diary pausedDiary() {
        Diary d = Diary.create(owner, LocalDate.now());
        ReflectionTestUtils.setField(d, "id", DIARY_ID);
        LocalDateTime now = LocalDateTime.now();
        d.pause(now, now.plusHours(24));
        return d;
    }

    private Diary deletedDiary() {
        Diary d = Diary.create(owner, LocalDate.now().minusDays(1));
        d.complete();
        d.delete();
        ReflectionTestUtils.setField(d, "id", DIARY_ID);
        return d;
    }

    private void assertError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable call,
            ErrorCode expected
    ) {
        assertThatThrownBy(call)
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
