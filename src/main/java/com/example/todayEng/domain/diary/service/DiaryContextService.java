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
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryContextService {

    private final DiaryRepository diaryRepository;
    private final DiaryContextPersistenceService persistenceService;
    private final ExternalAccountRepository externalAccountRepository;
    private final DiaryContextDataClient contextDataClient;
    private final DiaryImageAnalysisClient imageAnalysisClient;
    private final ImageUploadValidator imageUploadValidator;
    private final DiaryMemoryService diaryMemoryService;
    private final ObjectMapper objectMapper;

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

        List<MultipartFile> validatedImages = imageUploadValidator.validate(images);
        validateLocation(request.location());
        List<DiaryContext> contexts = new ArrayList<>();

        if (request.memo() != null) {
            contexts.add(persistenceService.saveMemo(userId, diaryId, request.memo(),
                    objectMapper.valueToTree(new MemoData(request.memo()))));
        }
        if (!validatedImages.isEmpty()) {
            contexts.add(collect(userId, diaryId, DiaryContextType.PHOTO,
                    () -> imageAnalysisClient.analyze(validatedImages)));
        }
        if (request.location() != null) {
            contexts.add(collect(userId, diaryId, DiaryContextType.WEATHER,
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
            contexts.add(collect(userId, diaryId, DiaryContextType.CALENDAR,
                    () -> contextDataClient.fetchCalendar(
                            account.getAccessToken(), diary.getDiaryDate())));
        } else if (account.getProvider() == ExternalServiceProvider.SPOTIFY) {
            contexts.add(collect(userId, diaryId, DiaryContextType.SPOTIFY,
                    () -> contextDataClient.fetchSpotify(
                            account.getAccessToken(), diary.getDiaryDate())));
        }
    }

    private DiaryContext collect(
            Long userId,
            Long diaryId,
            DiaryContextType type,
            Supplier<JsonNode> collector
    ) {
        try {
            JsonNode data = collector.get();
            if (data == null) {
                throw new IllegalStateException("Context collector returned no data");
            }
            return persistenceService.saveSuccess(userId, diaryId, type, data);
        } catch (RuntimeException exception) {
            log.warn("Diary context collection failed: diaryId={}, type={}, exception={}",
                    diaryId, type, exception.getClass().getSimpleName());
            return persistenceService.saveFailure(userId, diaryId, type);
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
