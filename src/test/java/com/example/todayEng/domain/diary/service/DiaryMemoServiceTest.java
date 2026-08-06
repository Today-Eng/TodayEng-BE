package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.todayEng.domain.diary.dto.request.DiaryMemoUpdateRequest;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import jakarta.validation.Validation;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DiaryMemoServiceTest {

    @Mock
    private DiaryRepository diaryRepository;

    @InjectMocks
    private DiaryMemoService diaryMemoService;

    private User user;
    private Diary completedDiary;

    @BeforeEach
    void setUp() {
        user = User.create();
        ReflectionTestUtils.setField(user, "id", 1L);

        completedDiary = Diary.create(user, LocalDate.now().minusDays(1));
        completedDiary.complete("기존 메모");
        ReflectionTestUtils.setField(completedDiary, "id", 10L);
    }

    @Test
    void updatesMemoForCompletedDiary() {
        given(diaryRepository.findByIdForUpdate(10L))
                .willReturn(Optional.of(completedDiary));

        var response = diaryMemoService.updateMemo(
                1L,
                10L,
                new DiaryMemoUpdateRequest("  수정된 메모  ")
        );

        assertThat(response.diaryId()).isEqualTo(10L);
        assertThat(response.memo()).isEqualTo("수정된 메모");
        assertThat(completedDiary.getMemo()).isEqualTo("수정된 메모");
    }

    @Test
    void stripsUnicodeWhitespaceInMemoUpdate() {
        given(diaryRepository.findByIdForUpdate(10L))
                .willReturn(Optional.of(completedDiary));

        var response = diaryMemoService.updateMemo(
                1L,
                10L,
                new DiaryMemoUpdateRequest("　수정된 메모　")
        );

        assertThat(response.memo()).isEqualTo("수정된 메모");
        assertThat(completedDiary.getMemo()).isEqualTo("수정된 메모");
    }

    @Test
    void normalizesNullAndBlankToNull() {
        given(diaryRepository.findByIdForUpdate(10L))
                .willReturn(Optional.of(completedDiary));

        var blankResponse = diaryMemoService.updateMemo(
                1L,
                10L,
                new DiaryMemoUpdateRequest("　　")
        );

        assertThat(blankResponse.memo()).isNull();

        var nullResponse = diaryMemoService.updateMemo(
                1L,
                10L,
                new DiaryMemoUpdateRequest(null)
        );

        assertThat(nullResponse.memo()).isNull();
        assertThat(completedDiary.getMemo()).isNull();
    }

    @Test
    void rejectsOtherOwner() {
        given(diaryRepository.findByIdForUpdate(10L))
                .willReturn(Optional.of(completedDiary));

        assertThatThrownBy(() -> diaryMemoService.updateMemo(
                2L,
                10L,
                new DiaryMemoUpdateRequest("메모")
        ))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void rejectsInProgressDiary() {
        Diary inProgressDiary = Diary.create(user, LocalDate.now());
        ReflectionTestUtils.setField(inProgressDiary, "id", 20L);
        given(diaryRepository.findByIdForUpdate(20L))
                .willReturn(Optional.of(inProgressDiary));

        assertThatThrownBy(() -> diaryMemoService.updateMemo(
                1L,
                20L,
                new DiaryMemoUpdateRequest("메모")
        ))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DIARY_NOT_COMPLETED);
    }

    @Test
    void memoLengthIsValidatedByDtoConstraint() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(
                    new DiaryMemoUpdateRequest("x".repeat(DiaryMemoUpdateRequest.MAX_MEMO_LENGTH + 1))
            );

            assertThat(violations).hasSize(1);
        }
    }
}
