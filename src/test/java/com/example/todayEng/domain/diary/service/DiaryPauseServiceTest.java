package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DiaryPauseServiceTest {

    @Mock private DiaryRepository diaryRepository;
    @InjectMocks private DiaryPauseService diaryPauseService;

    @Test
    void ownerCanEndInProgressDiaryRegardlessOfAnswerCount() {
        User user = user(1L);
        Diary diary = Diary.create(user, LocalDate.now());
        ReflectionTestUtils.setField(diary, "id", 10L);
        given(diaryRepository.findByIdForUpdate(10L)).willReturn(Optional.of(diary));

        var response = diaryPauseService.pause(1L, 10L);

        assertThat(response.diaryId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(DiaryStatus.COMPLETED);
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void otherUserCannotPauseDiary() {
        Diary diary = Diary.create(user(1L), LocalDate.now());
        given(diaryRepository.findByIdForUpdate(10L)).willReturn(Optional.of(diary));

        assertError(() -> diaryPauseService.pause(2L, 10L), ErrorCode.ACCESS_DENIED);
    }

    @Test
    void completedDiaryCanBeEndedAgainIdempotently() {
        Diary diary = Diary.create(user(1L), LocalDate.now());
        diary.complete();
        given(diaryRepository.findByIdForUpdate(10L)).willReturn(Optional.of(diary));

        var response = diaryPauseService.pause(1L, 10L);

        assertThat(response.status()).isEqualTo(DiaryStatus.COMPLETED);
        assertThat(response.completedAt()).isEqualTo(diary.getCompletedAt());
    }

    private User user(Long id) {
        User user = User.create();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode errorCode) {
        assertThatThrownBy(call)
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }
}
