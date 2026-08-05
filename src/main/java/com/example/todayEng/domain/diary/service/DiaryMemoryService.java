package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.client.DiaryMemoryAnalysisClient;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryMemoryService {

    private final DiaryMemoryReader reader;
    private final DiaryMemoryAnalysisClient analysisClient;
    private final DiaryMemoryResultValidator resultValidator;
    private final DiaryMemoryPersistenceService persistenceService;

    public Optional<DiaryContext> create(Long userId, Long currentDiaryId) {
        Optional<DiaryMemoryAnalysisCommand> prepared = reader.prepare(
                userId,
                currentDiaryId
        );
        if (prepared.isEmpty()) {
            persistenceService.clearExisting(userId, currentDiaryId);
            return Optional.empty();
        }

        Optional<DiaryMemoryResultValidator.ValidatedMemory> validated;
        try {
            DiaryMemoryAnalysisCommand command = prepared.get();
            DiaryMemoryAnalysisResponse response = analysisClient.analyze(command);
            validated = resultValidator.validate(command, response);
        } catch (RuntimeException exception) {
            log.warn(
                    "Diary memory analysis failed: diaryId={}, exception={}",
                    currentDiaryId,
                    exception.getClass().getSimpleName()
            );
            return Optional.of(
                    persistenceService.saveFailure(userId, currentDiaryId)
            );
        }
        if (validated.isEmpty()) {
            persistenceService.clearExisting(userId, currentDiaryId);
            return Optional.empty();
        }
        DiaryMemoryResultValidator.ValidatedMemory memory = validated.get();
        return Optional.of(persistenceService.saveSuccess(
                userId,
                currentDiaryId,
                memory.response(),
                memory.sourceDiaryIds()
        ));
    }
}
