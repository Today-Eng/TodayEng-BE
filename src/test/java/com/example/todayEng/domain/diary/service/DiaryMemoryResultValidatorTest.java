package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand.DiaryInput;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse.MemoryItem;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiaryMemoryResultValidatorTest {

    private final DiaryMemoryResultValidator validator =
            new DiaryMemoryResultValidator();

    @Test
    void acceptsRecurringMemoryWithAllowedSources() {
        var response = response(
                List.of(new MemoryItem("Seongsu", List.of(1L, 2L))),
                List.of(new MemoryItem("hopeful", List.of(2L)))
        );

        var result = validator.validate(command(), response);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().sourceDiaryIds())
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void rejectsRecurringMemoryFromOnlyOneDiary() {
        var response = response(
                List.of(new MemoryItem("Seongsu", List.of(1L))),
                List.of()
        );

        assertInvalid(() -> validator.validate(command(), response));
    }

    @Test
    void rejectsHallucinatedSourceDiaryId() {
        var response = response(
                List.of(new MemoryItem("Seongsu", List.of(1L, 999L))),
                List.of()
        );

        assertInvalid(() -> validator.validate(command(), response));
    }

    @Test
    void returnsEmptyWhenNoMeaningfulMemoryExists() {
        var response = response(List.of(), List.of());

        assertThat(validator.validate(command(), response)).isEmpty();
    }

    private DiaryMemoryAnalysisCommand command() {
        return new DiaryMemoryAnalysisCommand(
                10L,
                List.of(
                        new DiaryInput(1L, LocalDate.of(2026, 7, 1),
                                null, List.of()),
                        new DiaryInput(2L, LocalDate.of(2026, 7, 2),
                                null, List.of())
                )
        );
    }

    private DiaryMemoryAnalysisResponse response(
            List<MemoryItem> places,
            List<MemoryItem> emotions
    ) {
        return new DiaryMemoryAnalysisResponse(
                List.of(),
                places,
                List.of(),
                List.of(),
                emotions
        );
    }

    private void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_LLM_RESPONSE));
    }
}
