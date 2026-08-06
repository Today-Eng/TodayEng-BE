package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.diary.client.DiaryMemoryAnalysisClient;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand.DiaryInput;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse.MemoryItem;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiaryMemoryServiceTest {

    @Mock DiaryMemoryReader reader;
    @Mock DiaryMemoryAnalysisClient analysisClient;
    @Mock DiaryMemoryPersistenceService persistenceService;
    @Mock DiaryContext context;
    DiaryMemoryResultValidator validator = new DiaryMemoryResultValidator();
    DiaryMemoryService service;

    @BeforeEach
    void setUp() {
        service = new DiaryMemoryService(
                reader,
                analysisClient,
                validator,
                persistenceService
        );
    }

    @Test
    void skipsAnalysisWhenPastDiaryDoesNotExist() {
        given(reader.prepare(1L, 10L)).willReturn(Optional.empty());

        assertThat(service.create(1L, 10L)).isEmpty();
        verify(persistenceService).clearExisting(1L, 10L);
        verify(analysisClient, never()).analyze(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void savesValidatedMemoryAndSources() {
        var command = command();
        var response = response();
        given(reader.prepare(1L, 10L)).willReturn(Optional.of(command));
        given(analysisClient.analyze(command)).willReturn(response);
        given(persistenceService.saveSuccess(
                1L, 10L, response, Set.of(1L, 2L)
        )).willReturn(context);

        assertThat(service.create(1L, 10L)).contains(context);
    }

    @Test
    void storesFailureWhenGeminiCallFails() {
        var command = command();
        given(reader.prepare(1L, 10L)).willReturn(Optional.of(command));
        given(analysisClient.analyze(command))
                .willThrow(new RuntimeException("timeout"));
        given(persistenceService.saveFailure(1L, 10L)).willReturn(context);

        assertThat(service.create(1L, 10L)).contains(context);
        verify(persistenceService).saveFailure(1L, 10L);
    }

    @Test
    void doesNotCreateContextWhenResultIsEmpty() {
        var command = command();
        var empty = new DiaryMemoryAnalysisResponse(
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
        given(reader.prepare(1L, 10L)).willReturn(Optional.of(command));
        given(analysisClient.analyze(command)).willReturn(empty);

        assertThat(service.create(1L, 10L)).isEmpty();
        verify(persistenceService).clearExisting(1L, 10L);
        verify(persistenceService, never()).saveSuccess(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private DiaryMemoryAnalysisCommand command() {
        return new DiaryMemoryAnalysisCommand(
                10L,
                List.of(
                        new DiaryInput(1L, LocalDate.of(2026, 7, 1),
                                "first diary", List.of()),
                        new DiaryInput(2L, LocalDate.of(2026, 7, 2),
                                "second diary", List.of())
                )
        );
    }

    private DiaryMemoryAnalysisResponse response() {
        return new DiaryMemoryAnalysisResponse(
                List.of(),
                List.of(new MemoryItem("Seongsu", List.of(1L, 2L))),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
