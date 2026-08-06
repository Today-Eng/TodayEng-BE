package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse.MemoryItem;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class DiaryMemoryResultValidator {

    private static final int MAX_ITEMS_PER_CATEGORY = 5;
    private static final int MAX_VALUE_LENGTH = 200;

    public Optional<ValidatedMemory> validate(
            DiaryMemoryAnalysisCommand command,
            DiaryMemoryAnalysisResponse response
    ) {
        if (response == null) {
            throw invalidResponse();
        }

        Set<Long> allowedIds = command.diaries().stream()
                .map(DiaryMemoryAnalysisCommand.DiaryInput::diaryId)
                .collect(java.util.stream.Collectors.toSet());
        validateItems(response.people(), allowedIds, 2);
        validateItems(response.places(), allowedIds, 2);
        validateItems(response.themes(), allowedIds, 2);
        validateItems(response.ongoingStories(), allowedIds, 2);
        validateItems(response.recentEmotions(), allowedIds, 1);

        List<MemoryItem> allItems = Stream.of(
                        response.people(),
                        response.places(),
                        response.themes(),
                        response.ongoingStories(),
                        response.recentEmotions()
                )
                .flatMap(List::stream)
                .toList();
        if (allItems.isEmpty()) {
            return Optional.empty();
        }

        Set<Long> sourceDiaryIds = allItems.stream()
                .flatMap(item -> item.sourceDiaryIds().stream())
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new
                ));
        return Optional.of(new ValidatedMemory(response, sourceDiaryIds));
    }

    private void validateItems(
            List<MemoryItem> items,
            Set<Long> allowedIds,
            int minimumSourceCount
    ) {
        if (items.size() > MAX_ITEMS_PER_CATEGORY) {
            throw invalidResponse();
        }
        for (MemoryItem item : items) {
            if (item == null || item.value() == null
                    || item.value().isBlank()
                    || item.value().length() > MAX_VALUE_LENGTH) {
                throw invalidResponse();
            }
            Set<Long> sourceIds = new LinkedHashSet<>(
                    item.sourceDiaryIds()
            );
            if (item.sourceDiaryIds().size() > allowedIds.size()
                    || sourceIds.size() != item.sourceDiaryIds().size()
                    || sourceIds.size() < minimumSourceCount
                    || sourceIds.contains(null)
                    || !allowedIds.containsAll(sourceIds)) {
                throw invalidResponse();
            }
        }
    }

    private BaseException invalidResponse() {
        return new BaseException(ErrorCode.INVALID_LLM_RESPONSE);
    }

    public record ValidatedMemory(
            DiaryMemoryAnalysisResponse response,
            Set<Long> sourceDiaryIds
    ) {
    }
}
