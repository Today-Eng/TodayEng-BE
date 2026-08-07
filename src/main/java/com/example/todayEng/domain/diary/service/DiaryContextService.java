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
        if (!claimContextCollection(diaryId, userId)) {
            throw new BaseException(resolveClaimFailureReason(userId, diaryId));
        }

        boolean completed = false;
        try {
            DiaryContextCreateResponse response = collectContexts(
                    userId, diaryId, diary, request, images);
            persistenceService.completeContextCollection(userId, diaryId);
            completed = true;
            return response;
        } finally {
            if (!completed) {
                persistenceService.failContextCollection(userId, diaryId);
            }
        }
    }

    private boolean claimContextCollection(Long diaryId, Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (persistenceService.claimContextCollection(diaryId, userId, now)) {
            return true;
        }
        return persistenceService.reclaimStaleContextCollection(
                diaryId, userId, now, now.minus(CLAIM_STALE_AFTER));
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
            Diary diary,
            DiaryContextCreateRequest request,
            List<MultipartFile> images
    ) {
        List<MultipartFile> validatedImages = imageUploadValidator.validate(images);
        validateLocation(request.location());
        List<DiaryContext> contexts = new ArrayList<>();

        if (request.memo() != null) {
            contexts.add(persistenceService.saveMemo(userId, diaryId, request.memo(),
                    objectMapper.valueToTree(new MemoData(request.memo()))));
        }
        if (!validatedImages.isEmpty()) {
            contexts.add(collect(userId, diaryId, diary.getDiaryDate(), DiaryContextType.PHOTO,
                    () -> imageAnalysisClient.analyze(validatedImages)));
        }
        if (request.location() != null) {
            contexts.add(collect(userId, diaryId, diary.getDiaryDate(), DiaryContextType.WEATHER,
                    () -> contextDataClient.fetchWeather(
                            request.location(), diary.getDiaryDate())));
        }
        diaryMemoryService.create(userId, diaryId).ifPresent(contexts::add);

        externalAccountRepository.findAllByUser_Id(userId).stream()
                .filter(ExternalAccount::isUseEnabled)
                .forEach(account -> collectExternal(
                        userId, diaryId, diary, account, contexts));

        return new DiaryContextCreateResponse(diary.getId(), contexts.stream()
                .map(DiaryContextCreateResponse.ContextResult::from).toList());
    }

    private void collectExternal(
            Long userId,
            Long diaryId,
            Diary diary,
            ExternalAccount account,
            List<DiaryContext> contexts
    ) {
        if (account.getProvider() == ExternalServiceProvider.GOOGLE_CALENDAR) {
            contexts.add(collect(userId, diaryId, diary.getDiaryDate(), DiaryContextType.CALENDAR,
                    () -> contextDataClient.fetchCalendar(
                            account.getAccessToken(), diary.getDiaryDate())));
        } else if (account.getProvider() == ExternalServiceProvider.SPOTIFY) {
            contexts.add(collect(userId, diaryId, diary.getDiaryDate(), DiaryContextType.SPOTIFY,
                    () -> collectSpotify(userId, diary, account)));
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

    private DiaryContext collect(
            Long userId,
            Long diaryId,
            LocalDate diaryDate,
            DiaryContextType type,
            Supplier<JsonNode> collector
    ) {
        try {
            JsonNode data = collector.get();
            if (data == null) {
                throw new IllegalStateException("Context collector returned no data");
            }
            DiaryContext context = persistenceService.saveSuccess(userId, diaryId, type, data);
            cleanupSnapshot(userId, diaryDate, type);
            return context;
        } catch (RuntimeException exception) {
            log.warn("Diary context collection failed: diaryId={}, type={}, exception={}",
                    diaryId, type, exception.getClass().getSimpleName());
            return persistenceService.saveFailure(userId, diaryId, type);
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

    private void validateLocation(DiaryContextCreateRequest.Location location) {
        if (location != null
                && (location.latitude() < -90 || location.latitude() > 90
                || location.longitude() < -180 || location.longitude() > 180)) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private record MemoData(String memo) {
    }
}
