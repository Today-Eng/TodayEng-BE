package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextCollectionStatus;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryContextRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryContextPersistenceService {

    private final DiaryRepository diaryRepository;
    private final DiaryContextRepository contextRepository;
    private final Clock clock;

    @Transactional
    public Optional<DiaryContext> saveMemo(
            Long userId,
            Long diaryId,
            long leaseVersion,
            String memo,
            JsonNode data
    ) {
        if (!verifyLease(diaryId, userId, leaseVersion)) {
            return Optional.empty();
        }
        Diary diary = getManagedDiary(userId, diaryId);
        diary.updateMemo(memo);
        return Optional.of(saveSuccess(diary, DiaryContextType.MEMO, data));
    }

    @Transactional
    public Optional<DiaryContext> saveSuccess(
            Long userId,
            Long diaryId,
            long leaseVersion,
            DiaryContextType type,
            JsonNode data
    ) {
        if (!verifyLease(diaryId, userId, leaseVersion)) {
            return Optional.empty();
        }
        return Optional.of(saveSuccess(getManagedDiary(userId, diaryId), type, data));
    }

    @Transactional
    public Optional<DiaryContext> saveFailure(
            Long userId,
            Long diaryId,
            long leaseVersion,
            DiaryContextType type
    ) {
        if (!verifyLease(diaryId, userId, leaseVersion)) {
            return Optional.empty();
        }
        Diary diary = getManagedDiary(userId, diaryId);
        DiaryContext context = contextRepository
                .findByDiaryAndContextTypeAndContextKey(diary, type, 0)
                .orElseGet(() -> DiaryContext.failure(diary, type));
        context.markFailed();
        return Optional.of(contextRepository.save(context));
    }

    @Transactional
    public List<DiaryContext> savePhotoContexts(
            Long userId,
            Long diaryId,
            long leaseVersion,
            List<JsonNode> contextData
    ) {
        if (!verifyLease(diaryId, userId, leaseVersion)) {
            return List.of();
        }
        Diary diary = getManagedDiary(userId, diaryId);
        List<DiaryContext> existingContexts = contextRepository
                .findAllByDiaryAndContextTypeOrderByContextKey(
                        diary, DiaryContextType.PHOTO);
        List<DiaryContext> contexts = new ArrayList<>();
        for (int contextKey = 0; contextKey < contextData.size(); contextKey++) {
            int key = contextKey;
            JsonNode data = contextData.get(contextKey);
            DiaryContext context = contextRepository
                    .findByDiaryAndContextTypeAndContextKey(
                            diary, DiaryContextType.PHOTO, contextKey)
                    .orElseGet(() -> DiaryContext.success(
                            diary, DiaryContextType.PHOTO, key, data));
            context.updateContextData(data);
            contexts.add(contextRepository.save(context));
        }
        existingContexts.stream()
                .filter(context -> context.getContextKey() >= contextData.size())
                .forEach(DiaryContext::markFailed);
        return List.copyOf(contexts);
    }

    @Transactional
    public Optional<ContextCollectionClaim> claimContextCollection(
            Long diaryId,
            Long userId,
            LocalDateTime now
    ) {
        if (diaryRepository.claimContextCollection(diaryId, userId, now) != 1) {
            return Optional.empty();
        }
        return currentLease(diaryId, userId);
    }

    @Transactional
    public Optional<ContextCollectionClaim> reclaimStaleContextCollection(
            Long diaryId,
            Long userId,
            LocalDateTime now,
            LocalDateTime staleBefore
    ) {
        if (diaryRepository.reclaimStaleContextCollection(
                diaryId, userId, now, staleBefore) != 1) {
            return Optional.empty();
        }
        return currentLease(diaryId, userId);
    }

    @Transactional
    public void completeContextCollection(Long userId, Long diaryId, long leaseVersion) {
        finishContextCollection(userId, diaryId, leaseVersion, DiaryContextCollectionStatus.COMPLETED);
    }

    @Transactional
    public void failContextCollection(Long userId, Long diaryId, long leaseVersion) {
        finishContextCollection(userId, diaryId, leaseVersion, DiaryContextCollectionStatus.FAILED);
    }

    private void finishContextCollection(
            Long userId,
            Long diaryId,
            long leaseVersion,
            DiaryContextCollectionStatus targetStatus
    ) {
        int updated = diaryRepository.finishContextCollectionIfOwned(
                diaryId, userId, targetStatus, leaseVersion);
        if (updated == 0) {
            log.warn("Context collection lease lost: diaryId={}, expectedLeaseVersion={}, "
                    + "targetStatus={}", diaryId, leaseVersion, targetStatus);
        }
    }

    private boolean verifyLease(Long diaryId, Long userId, long leaseVersion) {
        int touched = diaryRepository.verifyContextCollectionLease(
                diaryId, userId, LocalDateTime.now(clock), leaseVersion);
        if (touched == 0) {
            log.warn("Context collection lease lost: diaryId={}, expectedLeaseVersion={}",
                    diaryId, leaseVersion);
            return false;
        }
        return true;
    }

    private Optional<ContextCollectionClaim> currentLease(Long diaryId, Long userId) {
        return diaryRepository.findByIdAndUserId(diaryId, userId)
                .map(diary -> new ContextCollectionClaim(diary.getContextCollectionLeaseVersion()));
    }

    private DiaryContext saveSuccess(Diary diary, DiaryContextType type, JsonNode data) {
        DiaryContext context = contextRepository
                .findByDiaryAndContextTypeAndContextKey(diary, type, 0)
                .orElseGet(() -> DiaryContext.success(diary, type, data));
        context.updateContextData(data);
        return contextRepository.save(context);
    }

    private Diary getManagedDiary(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findByIdAndUserId(diaryId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENTITY_NOT_FOUND));
        if (diary.getStatus() != DiaryStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.DIARY_ALREADY_COMPLETED);
        }
        return diary;
    }

    public record ContextCollectionClaim(long leaseVersion) {
    }
}
