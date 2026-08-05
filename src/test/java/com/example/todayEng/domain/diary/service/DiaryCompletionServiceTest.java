package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.example.todayEng.domain.diary.dto.request.DiaryCompleteRequest;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.*;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import jakarta.validation.Validation;
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
class DiaryCompletionServiceTest {
    @Mock DiaryRepository diaryRepository;
    @Mock DiaryQuestionRepository questionRepository;
    @Mock DiaryAnswerRepository answerRepository;
    @Mock DiaryCompletionPolicy completionPolicy;
    @InjectMocks DiaryCompletionService service;
    User user; Diary diary;

    @BeforeEach void setUp() {
        user = User.create(); ReflectionTestUtils.setField(user, "id", 1L);
        diary = Diary.create(user, LocalDate.now()); ReflectionTestUtils.setField(diary, "id", 2L);
    }

    @Test void completesAndStoresFinalMemoInDiaryMemo() {
        when(diaryRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(diary));
        when(questionRepository.findAllByDiaryIdInReflectionOrder(2L)).thenReturn(List.of());
        when(answerRepository.findAllByDiaryIdInReflectionOrder(2L)).thenReturn(List.of());
        var result = service.complete(1L, 2L, new DiaryCompleteRequest(" 최종 메모 "));
        assertThat(result.status()).isEqualTo(DiaryStatus.COMPLETED);
        assertThat(result.finalMemo()).isEqualTo("최종 메모");
        assertThat(result.completedAt()).isNotNull();
        assertThat(diary.getMemo()).isEqualTo("최종 메모");
        verify(completionPolicy).validate(anyList(), anyList());
    }

    @Test void acceptsNullAndNormalizesBlankToNull() {
        when(diaryRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(diary));
        when(questionRepository.findAllByDiaryIdInReflectionOrder(2L)).thenReturn(List.of());
        when(answerRepository.findAllByDiaryIdInReflectionOrder(2L)).thenReturn(List.of());
        assertThat(service.complete(1L, 2L, new DiaryCompleteRequest("  ")).finalMemo()).isNull();
    }

    @Test void alreadyCompletedIsIdempotentAndDoesNotOverwriteFinalMemo() {
        diary.complete("first");
        when(diaryRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(diary));
        var result = service.complete(1L, 2L, new DiaryCompleteRequest("second"));
        assertThat(result.finalMemo()).isEqualTo("first");
        verifyNoInteractions(questionRepository, answerRepository, completionPolicy);
    }

    @Test void rejectsOtherOwner() {
        when(diaryRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(diary));
        assertThatThrownBy(() -> service.complete(9L, 2L, new DiaryCompleteRequest(null)))
                .isInstanceOfSatisfying(BaseException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test void finalMemoLengthIsValidatedByDtoConstraint() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(
                    new DiaryCompleteRequest("x".repeat(DiaryCompleteRequest.MAX_FINAL_MEMO_LENGTH + 1)));
            assertThat(violations).hasSize(1);
        }
    }
}
