package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.DiaryContextSource;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryContextRepository;
import com.example.todayEng.domain.diary.repository.DiaryContextSourceRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiaryMemoryPersistenceService {

    private final DiaryRepository diaryRepository;
    private final DiaryContextRepository contextRepository;
    private final DiaryContextSourceRepository sourceRepository;
    private final ObjectMapper objectMapper;

    public DiaryMemoryPersistenceService(
            DiaryRepository diaryRepository,
            DiaryContextRepository contextRepository,
            DiaryContextSourceRepository sourceRepository,
            ObjectMapper objectMapper
    ) {
        this.diaryRepository = diaryRepository;
        this.contextRepository = contextRepository;
        this.sourceRepository = sourceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DiaryContext saveSuccess(
            Long userId,
            Long currentDiaryId,
            DiaryMemoryAnalysisResponse response,
            Set<Long> sourceDiaryIds
    ) {
        Diary currentDiary = getLockedCurrentDiary(userId, currentDiaryId);
        List<Diary> sources = diaryRepository.findAllById(sourceDiaryIds);
        validateSources(userId, currentDiaryId, sourceDiaryIds, sources);

        DiaryContext context = contextRepository
                .findByDiaryAndContextType(
                        currentDiary,
                        DiaryContextType.DIARY_MEMORY
                )
                .orElseGet(() -> DiaryContext.success(
                        currentDiary,
                        DiaryContextType.DIARY_MEMORY,
                        objectMapper.valueToTree(response)
                ));
        context.updateContextData(objectMapper.valueToTree(response));
        DiaryContext saved = contextRepository.saveAndFlush(context);

        sourceRepository.deleteAllByContextId(saved.getId());
        sourceRepository.saveAll(sources.stream()
                .map(source -> DiaryContextSource.create(saved, source))
                .toList());
        return saved;
    }

    @Transactional
    public DiaryContext saveFailure(Long userId, Long currentDiaryId) {
        Diary currentDiary = getLockedCurrentDiary(userId, currentDiaryId);
        DiaryContext context = contextRepository
                .findByDiaryAndContextType(
                        currentDiary,
                        DiaryContextType.DIARY_MEMORY
                )
                .orElseGet(() -> DiaryContext.failure(
                        currentDiary,
                        DiaryContextType.DIARY_MEMORY
                ));
        context.markFailed();
        DiaryContext saved = contextRepository.saveAndFlush(context);
        sourceRepository.deleteAllByContextId(saved.getId());
        return saved;
    }

    @Transactional
    public void clearExisting(Long userId, Long currentDiaryId) {
        Diary currentDiary = getLockedCurrentDiary(userId, currentDiaryId);
        contextRepository.findByDiaryAndContextType(
                currentDiary,
                DiaryContextType.DIARY_MEMORY
        ).ifPresent(context -> {
            context.markFailed();
            sourceRepository.deleteAllByContextId(context.getId());
        });
    }

    private Diary getLockedCurrentDiary(Long userId, Long currentDiaryId) {
        Diary diary = diaryRepository.findByIdForUpdate(currentDiaryId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENTITY_NOT_FOUND));
        if (!diary.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.ENTITY_NOT_FOUND);
        }
        if (diary.getStatus() != DiaryStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.DIARY_NOT_IN_PROGRESS);
        }
        return diary;
    }

    private void validateSources(
            Long userId,
            Long currentDiaryId,
            Set<Long> requestedIds,
            List<Diary> sources
    ) {
        boolean invalid = sources.size() != requestedIds.size()
                || sources.stream().anyMatch(source ->
                        source.getId().equals(currentDiaryId)
                        || !source.getUser().getId().equals(userId)
                        || source.getStatus() != DiaryStatus.COMPLETED
                );
        if (invalid) {
            throw new BaseException(ErrorCode.INVALID_LLM_RESPONSE);
        }
    }
}
