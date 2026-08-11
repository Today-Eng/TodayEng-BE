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
import com.example.todayEng.global.log.ExternalCallLog;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.CompletionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
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
    private final Executor contextExecutor;

    public DiaryContextService(
            DiaryRepository diaryRepository,
            DiaryContextPersistenceService persistenceService,
            ExternalAccountRepository externalAccountRepository,
            DiaryContextDataClient contextDataClient,
            DiaryImageAnalysisClient imageAnalysisClient,
            ImageUploadValidator imageUploadValidator,
            DiaryMemoryService diaryMemoryService,
            DailyContextSnapshotPersistenceService snapshotPersistenceService,
            ObjectMapper objectMapper,
            Clock clock,
            @Qualifier("diaryContextExecutor") Executor contextExecutor
    ) {
        this.diaryRepository = diaryRepository;
        this.persistenceService = persistenceService;
        this.externalAccountRepository = externalAccountRepository;
        this.contextDataClient = contextDataClient;
        this.imageAnalysisClient = imageAnalysisClient;
        this.imageUploadValidator = imageUploadValidator;
        this.diaryMemoryService = diaryMemoryService;
        this.snapshotPersistenceService = snapshotPersistenceService;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.contextExecutor = contextExecutor;
    }

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
        List<Supplier<Optional<DiaryContext>>> collectors = new ArrayList<>();
        if (!validatedImages.isEmpty()) {
            collectors.add(() -> collect(userId, diaryId, leaseVersion, diary.getDiaryDate(),
                    DiaryContextType.PHOTO, () -> imageAnalysisClient.analyze(validatedImages)));
        }
        if (request.location() != null) {
            collectors.add(() -> collect(userId, diaryId, leaseVersion, diary.getDiaryDate(),
                    DiaryContextType.WEATHER, () -> contextDataClient.fetchWeather(
                            request.location(), diary.getDiaryDate())));
        }
        collectors.add(() -> diaryMemoryService.create(userId, diaryId));
        externalAccountRepository.findAllByUser_Id(userId).stream()
                .filter(ExternalAccount::isUseEnabled)
                .map(account -> externalCollector(userId, diaryId, leaseVersion, diary, account))
                .filter(java.util.Objects::nonNull)
                .forEach(collectors::add);

        collectors.stream()
                .map(collector -> CompletableFuture.supplyAsync(collector, contextExecutor))
                .toList().stream()
                .map(this::joinContext)
                .flatMap(Optional::stream)
                .forEach(contexts::add);

        return new DiaryContextCreateResponse(diary.getId(), contexts.stream()
                .map(DiaryContextCreateResponse.ContextResult::from).toList());
    }

    private Supplier<Optional<DiaryContext>> externalCollector(
            Long userId,
            Long diaryId,
            long leaseVersion,
            Diary diary,
            ExternalAccount account
    ) {
        if (account.getProvider() == ExternalServiceProvider.GOOGLE_CALENDAR) {
            return () -> collect(userId, diaryId, leaseVersion, diary.getDiaryDate(), DiaryContextType.CALENDAR,
                    () -> contextDataClient.fetchCalendar(
                            account.getAccessToken(), diary.getDiaryDate()));
        } else if (account.getProvider() == ExternalServiceProvider.SPOTIFY) {
            return () -> collect(userId, diaryId, leaseVersion, diary.getDiaryDate(), DiaryContextType.SPOTIFY,
                    () -> collectSpotify(userId, diary, account));
        }
        return null;
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
            log.warn("Spotify refresh failed, falling back to preloaded context: cause={}",
                    ExternalCallLog.describe(exception));
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
            JsonNode data = normalize(type, collector.get());
            if (data == null) {
                throw new IllegalStateException("Context collector returned no data");
            }
            Optional<DiaryContext> context = persistenceService.saveSuccess(
                    userId, diaryId, leaseVersion, type, data);
            context.ifPresent(ignored -> cleanupSnapshot(userId, diaryDate, type));
            return context;
        } catch (RuntimeException exception) {
            log.warn("Diary context collection failed: diaryId={}, type={}, cause={}",
                    diaryId, type, ExternalCallLog.describe(exception));
            return persistenceService.saveFailure(userId, diaryId, leaseVersion, type);
        }
    }

    private Optional<DiaryContext> joinContext(
            CompletableFuture<Optional<DiaryContext>> future
    ) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Error error) throw error;
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            throw exception;
        }
    }

    private JsonNode normalize(DiaryContextType type, JsonNode source) {
        if (source == null || type == DiaryContextType.MEMO
                || type == DiaryContextType.PHOTO || type == DiaryContextType.DIARY_MEMORY) {
            return source;
        }
        ObjectNode result = objectMapper.createObjectNode();
        if (type == DiaryContextType.WEATHER) {
            JsonNode daily = source.path("daily");
            result.put("weatherCode", firstInt(daily.path("weather_code")));
            result.put("maxTemperatureC", firstDouble(daily.path("temperature_2m_max")));
            result.put("minTemperatureC", firstDouble(daily.path("temperature_2m_min")));
            result.put("precipitationMm", firstDouble(daily.path("precipitation_sum")));
            return result;
        }
        if (type == DiaryContextType.CALENDAR) {
            ArrayNode events = result.putArray("events");
            source.path("items").forEach(item -> {
                ObjectNode event = events.addObject();
                event.put("title", item.path("summary").asText("일정"));
                event.put("start", temporalValue(item.path("start")));
                event.put("end", temporalValue(item.path("end")));
            });
            return result;
        }
        if (type == DiaryContextType.SPOTIFY) {
            ArrayNode plays = result.putArray("recentPlays");
            Map<String, Integer> counts = new LinkedHashMap<>();
            source.path("items").forEach(item -> {
                String track = item.path("track").path("name").asText("");
                String artist = item.path("track").path("artists").path(0).path("name").asText("");
                if (!track.isBlank()) counts.merge(track + " — " + artist, 1, Integer::sum);
            });
            counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .forEach(entry -> {
                        ObjectNode play = plays.addObject();
                        play.put("trackAndArtist", entry.getKey());
                        play.put("playCount", entry.getValue());
                    });
            result.put("totalPlays", source.path("items").size());
            return result;
        }
        return source;
    }

    private int firstInt(JsonNode node) {
        return node.isArray() && !node.isEmpty() ? node.path(0).asInt() : 0;
    }

    private double firstDouble(JsonNode node) {
        return node.isArray() && !node.isEmpty() ? node.path(0).asDouble() : 0;
    }

    private String temporalValue(JsonNode node) {
        return node.path("dateTime").asText(node.path("date").asText(""));
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
                && (!Double.isFinite(location.latitude())
                || !Double.isFinite(location.longitude())
                || location.latitude() < -90 || location.latitude() > 90
                || location.longitude() < -180 || location.longitude() > 180)) {
            throw new BaseException(ErrorCode.INVALID_DIARY_LOCATION);
        }
    }

    private record MemoData(String memo) {
    }
}
