package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.diary.dto.request.DiaryStartRequest;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {

    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DiaryService diaryService;

    private final Long userId = 1L;
    private final User user = User.create();

    @Test
    @DisplayName("오늘 날짜로 회고를 시작하면 새로운 회고가 생성된다")
    void startDiary_success() {
        LocalDate today = LocalDate.now(SERVICE_ZONE_ID);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(diaryRepository.findByUserAndDiaryDate(user, today)).willReturn(Optional.empty());
        given(diaryRepository.saveAndFlush(any(Diary.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = diaryService.startDiary(userId, new DiaryStartRequest(today));

        assertThat(response.diaryDate()).isEqualTo(today);
        assertThat(response.resumed()).isFalse();
    }

    @Test
    @DisplayName("미래 날짜로 회고를 시작하면 예외가 발생한다")
    void startDiary_futureDate_throws() {
        LocalDate tomorrow = LocalDate.now(SERVICE_ZONE_ID).plusDays(1);

        assertThatThrownBy(() -> diaryService.startDiary(userId, new DiaryStartRequest(tomorrow)))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FUTURE_DIARY_DATE_NOT_ALLOWED);

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("7일 이전 날짜로 회고를 시작하면 예외가 발생한다")
    void startDiary_expiredDate_throws() {
        LocalDate sevenDaysAgo = LocalDate.now(SERVICE_ZONE_ID).minusDays(7);

        assertThatThrownBy(() -> diaryService.startDiary(userId, new DiaryStartRequest(sevenDaysAgo)))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DIARY_DATE_EXPIRED);
    }

    @Test
    @DisplayName("작성 가능 기간의 마지막 날(6일 전)에는 회고를 시작할 수 있다")
    void startDiary_writablePeriodBoundary_success() {
        LocalDate sixDaysAgo = LocalDate.now(SERVICE_ZONE_ID).minusDays(6);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(diaryRepository.findByUserAndDiaryDate(user, sixDaysAgo)).willReturn(Optional.empty());
        given(diaryRepository.saveAndFlush(any(Diary.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = diaryService.startDiary(userId, new DiaryStartRequest(sixDaysAgo));

        assertThat(response.diaryDate()).isEqualTo(sixDaysAgo);
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 예외가 발생한다")
    void startDiary_userNotFound_throws() {
        LocalDate today = LocalDate.now(SERVICE_ZONE_ID);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> diaryService.startDiary(userId, new DiaryStartRequest(today)))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 진행 중인 회고가 있으면 예외가 발생한다")
    void startDiary_alreadyInProgress_throws() {
        LocalDate today = LocalDate.now(SERVICE_ZONE_ID);
        Diary inProgressDiary = Diary.create(user, today);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(diaryRepository.findByUserAndDiaryDate(user, today))
                .willReturn(Optional.of(inProgressDiary));

        assertThatThrownBy(() -> diaryService.startDiary(userId, new DiaryStartRequest(today)))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DIARY_ALREADY_IN_PROGRESS);

        verify(diaryRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("이미 완료된 회고가 있으면 예외가 발생한다")
    void startDiary_alreadyCompleted_throws() {
        LocalDate today = LocalDate.now(SERVICE_ZONE_ID);
        Diary completedDiary = Diary.create(user, today);
        completedDiary.complete();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(diaryRepository.findByUserAndDiaryDate(user, today))
                .willReturn(Optional.of(completedDiary));

        assertThatThrownBy(() -> diaryService.startDiary(userId, new DiaryStartRequest(today)))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DIARY_ALREADY_COMPLETED);
    }

    @Test
    @DisplayName("동시 생성 요청으로 (user_id, diary_date) 유니크 제약이 깨지면 진행 중 예외로 변환된다")
    void startDiary_concurrentDuplicate_throwsAlreadyInProgress() {
        LocalDate today = LocalDate.now(SERVICE_ZONE_ID);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(diaryRepository.findByUserAndDiaryDate(user, today)).willReturn(Optional.empty());
        given(diaryRepository.saveAndFlush(any(Diary.class)))
                .willThrow(uniqueConstraintViolation("uk_diary_user_date"));

        assertThatThrownBy(() -> diaryService.startDiary(userId, new DiaryStartRequest(today)))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DIARY_ALREADY_IN_PROGRESS);
    }

    @Test
    @DisplayName("다른 제약 조건 위반(예: FK, NOT NULL)은 변환하지 않고 그대로 전파한다")
    void startDiary_unrelatedConstraintViolation_propagatesAsIs() {
        LocalDate today = LocalDate.now(SERVICE_ZONE_ID);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(diaryRepository.findByUserAndDiaryDate(user, today)).willReturn(Optional.empty());
        given(diaryRepository.saveAndFlush(any(Diary.class)))
                .willThrow(uniqueConstraintViolation("fk_diary_user"));

        assertThatThrownBy(() -> diaryService.startDiary(userId, new DiaryStartRequest(today)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(BaseException.class);
    }

    private DataIntegrityViolationException uniqueConstraintViolation(String constraintName) {
        SQLException sqlException = new SQLException(
                "Duplicate entry for key '" + constraintName + "'"
        );
        ConstraintViolationException cause = new ConstraintViolationException(
                "could not execute statement",
                sqlException,
                constraintName
        );

        return new DataIntegrityViolationException("could not execute statement", cause);
    }
}
