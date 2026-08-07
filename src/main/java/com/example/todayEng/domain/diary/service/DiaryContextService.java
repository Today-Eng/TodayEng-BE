package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.client.DiaryContextDataClient;
import com.example.todayEng.domain.diary.client.DiaryImageAnalysisClient;
import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryContextCreateResponse;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.diary.service.DiaryContextPersistenceService.ContextCollectionClaim;
import com.example.todayEng.domain.home.service.DailyContextSnapshotPersistenceService;
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryContextService {

    private static final Duration CLAIM_STALE_AFTER = Duration.ofMinutes(10);
    private static final int MAX_MEMO_LENGTH = 200;

    private final DiaryRepository diaryRepository;
    private final DiaryContextPersistenceService persistenceService;
    private final ExternalAccountRepository externalAccountRepository;
    private final DiaryContextDataClient contextDataClient;
    private final DiaryImageAnalysisClient imageAnalysisClient;
    private final ImageUploadValidator imageUploadValidator;
    private final DiaryMemoryService diaryMemoryService;
    private final DailyContextSnapshotPersistenceService snapshotPersistenceService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DiaryContextCreateResponse createContexts(
            Long userId,
            Long diaryId,
            DiaryContextCreateRequest request,
            List<MultipartFile> images
    ) {
        Diary diary = diaryRepository.findByIdAndUserId(diaryId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENTITY_NOT_FOUND));
        if (diary.getStatus() != DiaryStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.DIARY_ALREADY_COMPLETED);
        }
        long leaseVersion = claimContextCollection(diaryId, userId)
                .orElseThrow(() -> new BaseException(resolveClaimFailureReason(userId, diaryId)));

        boolean completed = false;
        try {
            DiaryContextCreateResponse response = collectContexts(
                    userId, diaryId, leaseVersion, diary, request, images);
            persistenceService.completeContextCollection(userId, diaryId, leaseVersion);
            completed = true;
            return response;
        } finally {
            if (!completed) {
                persistenceService.failContextCollection(userId, diaryId, leaseVersion);
            }
        }
    }

    private Optional<Long> claimContextCollection(Long diaryId, Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<ContextCollectionClaim> claim =
                persistenceService.claimContextCollection(diaryId, userId, now);
        if (claim.isEmpty()) {
            claim = persistenceService.reclaimStaleContextCollection(
                    diaryId, userId, now, now.minus(CLAIM_STALE_AFTER));
        }
        return claim.map(ContextCollectionClaim::leaseVersion);
    }

    private ErrorCode resolveClaimFailureReason(Long userId, Long diaryId) {
        Diary current = diaryRepository.findByIdAndUserId(diaryId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENTITY_NOT_FOUND));
        if (current.getStatus() != DiaryStatus.IN_PROGRESS) {
            return ErrorCode.DIARY_ALREADY_COMPLETED;
        }
        return ErrorCode.DIARY_CONTEXT_ALREADY_GENERATED;
    }

    private DiaryContextCreateResponse collectContexts(
            Long userId,
            Long diaryId,
            long leaseVersion,
            Diary diary,
            DiaryContextCreateRequest request,
            List<MultipartFile> images
    ) {
        List<MultipartFile> validatedImages = imageUploadValidator.validate(images);
        validateMemo(request.memo());
        validateLocation(request.location());
        List<DiaryContext> contexts = new ArrayList<>();

        if (request.memo() != null) {
            persistenceService.saveMemo(userId, diaryId, leaseVersion, request.memo(),
                            objectMapper.valueToTree(new MemoData(request.memo())))
                    .ifPresent(contexts::add);
        }
        if (!validatedImages.isEmpty()) {
            collect(userId, diaryId, leaseVersion, diary.getDiaryDate(), DiaryContextType.PHOTO,
                    () -> imageAnalysisClient.analyze(validatedImages))
                    .ifPresent(contexts::add);
        }
        if (request.location() != null) {
            collect(userId, diaryId, leaseVersion, diary.getDiaryDate(), DiaryContextType.WEATHER,
                    () -> contextDataClient.fetchWeather(
                            request.location(), diary.getDiaryDate()))
                    .ifPresent(contexts::add);
        }
        diaryMemoryService.create(userId, diaryId).ifPresent(contexts::add);

        externalAccountRepository.findAllByUser_Id(userId).stream()
                .filter(ExternalAccount::isUseEnabled)
                .forEach(account -> collectExternal(
                        userId, diaryId, leaseVersion, diary, account, contexts));

        return new DiaryContextCreateResponse(diary.getId(), contexts.stream()
                .map(DiaryContextCreateResponse.ContextResult::from).toList());
    }

    private void collectExternal(
            Long userId,
            Long diaryId,
            long leaseVersion,
            Diary diary,
            ExternalAccount account,
            List<DiaryContext> contexts
    ) {
        if (account.getProvider() == ExternalServiceProvider.GOOGLE_CALENDAR) {
            collect(userId, diaryId, leaseVersion, diary.getDiaryDate(), DiaryContextType.CALENDAR,
                    () -> contextDataClient.fetchCalendar(
                            account.getAccessToken(), diary.getDiaryDate()))
                    .ifPresent(contexts::add);
        } else if (account.getProvider() == ExternalServiceProvider.SPOTIFY) {
            collect(userId, diaryId, leaseVersion, diary.getDiaryDate(), DiaryContextType.SPOTIFY,
                    () -> collectSpotify(userId, diary, account))
                    .ifPresent(contexts::add);
        }
    }

    private JsonNode collectSpotify(Long userId, Diary diary, ExternalAccount account) {
        JsonNode preload = findSpotifyPreload(userId, diary.getDiaryDate());
        JsonNode fresh;
        try {
            fresh = contextDataClient.fetchSpotify(
                    account.getAccessToken(), diary.getDiaryDate());
        } catch (RuntimeException exception) {
            if (preload == null) {
                throw exception;
            }
            log.warn("Spotify refresh failed, falling back to preloaded context: exception={}",
                    exception.getClass().getSimpleName());
            return preload;
        }
        if (fresh == null) {
            return preload;
        }
        if (preload == null) {
            return fresh;
        }
        return mergeSpotifyItems(preload, fresh);
    }

    private JsonNode findSpotifyPreload(Long userId, LocalDate date) {
        return snapshotPersistenceService
                .findSuccessfulContextData(userId, date, DiaryContextType.SPOTIFY)
                .orElse(null);
    }

    private JsonNode mergeSpotifyItems(JsonNode preload, JsonNode fresh) {
        Map<String, JsonNode> byPlayedAt = new LinkedHashMap<>();
        for (JsonNode source : List.of(preload, fresh)) {
            JsonNode items = source.path("items");
            if (items.isArray()) {
                items.forEach(item -> {
                    String playedAt = item.path("played_at").asText(null);
                    if (playedAt != null) {
                        byPlayedAt.put(playedAt, item);
                    }
                });
            }
        }
        ArrayNode merged = objectMapper.createArrayNode();
        byPlayedAt.values().stream()
                .sorted(Comparator.comparing(
                        (JsonNode item) -> item.path("played_at").asText()).reversed())
                .forEach(merged::add);
        ObjectNode result = fresh.isObject()
                ? fresh.deepCopy()
                : objectMapper.createObjectNode();
        result.set("items", merged);
        return result;
    }

    private Optional<DiaryContext> collect(
            Long userId,
            Long diaryId,
            long leaseVersion,
            LocalDate diaryDate,
            DiaryContextType type,
            Supplier<JsonNode> collector
    ) {
        try {
            JsonNode data = collector.get();
            if (data == null) {
                throw new IllegalStateException("Context collector returned no data");
            }
            Optional<DiaryContext> context = persistenceService.saveSuccess(
                    userId, diaryId, leaseVersion, type, data);
            context.ifPresent(ignored -> cleanupSnapshot(userId, diaryDate, type));
            return context;
        } catch (RuntimeException exception) {
            log.warn("Diary context collection failed: diaryId={}, type={}, exception={}, message={}",
                    diaryId, type, exception.getClass().getName(), exception.getMessage(),
                    exception);
            return persistenceService.saveFailure(userId, diaryId, leaseVersion, type);
        }
    }

    private void cleanupSnapshot(Long userId, LocalDate diaryDate, DiaryContextType type) {
        try {
            snapshotPersistenceService.cleanupCollected(userId, diaryDate, type);
        } catch (RuntimeException exception) {
            log.warn("Daily context snapshot cleanup failed: userId={}, diaryDate={}, type={}",
                    userId, diaryDate, type, exception);
        }
    }

    private void validateMemo(String memo) {
        if (memo != null && memo.length() > MAX_MEMO_LENGTH) {
            throw new BaseException(ErrorCode.DIARY_MEMO_TOO_LONG);
        }
    }

    private void validateLocation(DiaryContextCreateRequest.Location location) {
        if (location != null
                && (location.latitude() < -90 || location.latitude() > 90
                || location.longitude() < -180 || location.longitude() > 180)) {
            throw new BaseException(ErrorCode.INVALID_DIARY_LOCATION);
        }
    }

    private record MemoData(String memo) {
    }
}
