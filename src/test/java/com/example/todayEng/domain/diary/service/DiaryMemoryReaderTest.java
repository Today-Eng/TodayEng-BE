package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryAnswerRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class DiaryMemoryReaderTest {

    @Mock DiaryRepository diaryRepository;
    @Mock DiaryAnswerRepository answerRepository;
    DiaryMemoryReader reader;

    @BeforeEach
    void setUp() {
        reader = new DiaryMemoryReader(diaryRepository, answerRepository);
    }

    @Test
    void readsPreviousThirtyDaysUpToTenAndUsesCorrectedAnswer() {
        LocalDate currentDate = LocalDate.of(2026, 8, 5);
        Diary current = diary(10L, currentDate, null, DiaryStatus.IN_PROGRESS);
        Diary source = diary(1L, LocalDate.of(2026, 7, 20),
                "final memo", DiaryStatus.COMPLETED);
        DiaryQuestion question = mock(DiaryQuestion.class);
        DiaryAnswer answer = mock(DiaryAnswer.class);
        given(question.getDiary()).willReturn(source);
        given(question.getQuestionText()).willReturn("How was your day?");
        given(answer.getQuestion()).willReturn(question);
        given(answer.getCorrectedText()).willReturn("corrected");
        given(diaryRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(current));
        given(diaryRepository.findRecentCompletedForMemory(
                1L,
                10L,
                currentDate.minusDays(30),
                currentDate,
                PageRequest.of(0, 10)
        )).willReturn(List.of(source));
        given(answerRepository.findAllForMemoryAnalysis(List.of(1L)))
                .willReturn(List.of(answer));

        var command = reader.prepare(1L, 10L).orElseThrow();

        assertThat(command.diaries()).singleElement().satisfies(input -> {
            assertThat(input.diaryId()).isEqualTo(1L);
            assertThat(input.reflections()).singleElement().satisfies(reflection ->
                    assertThat(reflection.answer()).isEqualTo("corrected"));
        });
    }

    @Test
    void skipsAnswerQueryWhenPastDiaryDoesNotExist() {
        LocalDate currentDate = LocalDate.of(2026, 8, 5);
        Diary current = diary(10L, currentDate, null, DiaryStatus.IN_PROGRESS);
        given(diaryRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(current));
        given(diaryRepository.findRecentCompletedForMemory(
                any(), any(), any(), any(), any()
        )).willReturn(List.of());

        assertThat(reader.prepare(1L, 10L)).isEmpty();
        verify(answerRepository, never()).findAllForMemoryAnalysis(any());
    }

    @Test
    void truncatesMemoAtMaximumLength() {
        LocalDate currentDate = LocalDate.of(2026, 8, 5);
        Diary current = diary(10L, currentDate, null, DiaryStatus.IN_PROGRESS);
        Diary source = diary(1L, currentDate.minusDays(1),
                String.valueOf('a').repeat(2001), DiaryStatus.COMPLETED);
        given(diaryRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(current));
        given(diaryRepository.findRecentCompletedForMemory(
                any(), any(), any(), any(), any()
        )).willReturn(List.of(source));
        given(answerRepository.findAllForMemoryAnalysis(List.of(1L)))
                .willReturn(List.of());

        var command = reader.prepare(1L, 10L).orElseThrow();

        assertThat(command.diaries().get(0).memo()).hasSize(2000);
    }

    private Diary diary(
            Long id,
            LocalDate date,
            String memo,
            DiaryStatus status
    ) {
        Diary diary = mock(Diary.class);
        lenient().when(diary.getId()).thenReturn(id);
        lenient().when(diary.getDiaryDate()).thenReturn(date);
        lenient().when(diary.getMemo()).thenReturn(memo);
        lenient().when(diary.getStatus()).thenReturn(status);
        return diary;
    }
}
