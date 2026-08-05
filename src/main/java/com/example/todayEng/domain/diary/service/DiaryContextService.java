package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.client.DiaryContextDataClient;
import com.example.todayEng.domain.diary.client.DiaryImageAnalysisClient;
import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryContextCreateResponse;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryContextRepository;
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
import java.util.Set;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryContextService {

    private static final int MAX_IMAGE_COUNT = 2;
    private static final long MAX_IMAGE_SIZE = 7L * 1024 * 1024;
    private static final long MAX_TOTAL_IMAGE_SIZE = 14L * 1024 * 1024;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final DiaryRepository diaryRepository;
    private final DiaryContextRepository diaryContextRepository;
    private final ExternalAccountRepository externalAccountRepository;
    private final DiaryContextDataClient contextDataClient;
    private final DiaryImageAnalysisClient imageAnalysisClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public DiaryContextCreateResponse createContexts(
            Long userId,
            Long diaryId,
            DiaryContextCreateRequest request,
            List<MultipartFile> images
    ) {
        Diary diary = diaryRepository.findById(diaryId)
                .filter(found -> found.getUser().getId().equals(userId))
                .orElseThrow(() -> new BaseException(ErrorCode.ENTITY_NOT_FOUND));
        if (diary.getStatus() != DiaryStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.DIARY_ALREADY_COMPLETED);
        }

        List<MultipartFile> validatedImages = validateImages(images);
        validateLocation(request.location());
        List<DiaryContext> contexts = new ArrayList<>();

        if (request.memo() != null) {
            diary.updateMemo(request.memo());
            contexts.add(saveSuccess(diary, DiaryContextType.MEMO,
                    objectMapper.valueToTree(new MemoData(request.memo()))));
        }
        if (!validatedImages.isEmpty()) {
            contexts.add(collect(diary, DiaryContextType.PHOTO,
                    () -> imageAnalysisClient.analyze(validatedImages)));
        }
        if (request.location() != null) {
            contexts.add(collect(diary, DiaryContextType.WEATHER,
                    () -> contextDataClient.fetchWeather(
                            request.location(), diary.getDiaryDate())));
        }

        externalAccountRepository.findAllByUser_Id(userId).stream()
                .filter(ExternalAccount::isUseEnabled)
                .forEach(account -> collectExternal(diary, account, contexts));

        return new DiaryContextCreateResponse(diary.getId(), contexts.stream()
                .map(DiaryContextCreateResponse.ContextResult::from).toList());
    }

    private void collectExternal(
            Diary diary,
            ExternalAccount account,
            List<DiaryContext> contexts
    ) {
        if (account.getProvider() == ExternalServiceProvider.GOOGLE_CALENDAR) {
            contexts.add(collect(diary, DiaryContextType.CALENDAR,
                    () -> contextDataClient.fetchCalendar(
                            account.getAccessToken(), diary.getDiaryDate())));
        } else if (account.getProvider() == ExternalServiceProvider.SPOTIFY) {
            contexts.add(collect(diary, DiaryContextType.SPOTIFY,
                    () -> contextDataClient.fetchSpotify(
                            account.getAccessToken(), diary.getDiaryDate())));
        }
    }

    private DiaryContext collect(
            Diary diary,
            DiaryContextType type,
            Supplier<JsonNode> collector
    ) {
        try {
            JsonNode data = collector.get();
            if (data == null) {
                throw new IllegalStateException("Context collector returned no data");
            }
            return saveSuccess(diary, type, data);
        } catch (RuntimeException exception) {
            log.warn("Diary context collection failed: diaryId={}, type={}, exception={}",
                    diary.getId(), type, exception.getClass().getSimpleName());
            return saveFailure(diary, type);
        }
    }

    private DiaryContext saveSuccess(Diary diary, DiaryContextType type, JsonNode data) {
        DiaryContext context = diaryContextRepository.findByDiaryAndContextType(diary, type)
                .orElseGet(() -> DiaryContext.success(diary, type, data));
        context.updateContextData(data);
        return diaryContextRepository.save(context);
    }

    private DiaryContext saveFailure(Diary diary, DiaryContextType type) {
        DiaryContext context = diaryContextRepository.findByDiaryAndContextType(diary, type)
                .orElseGet(() -> DiaryContext.failure(diary, type));
        context.markFailed();
        return diaryContextRepository.save(context);
    }

    private List<MultipartFile> validateImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        if (images.size() > MAX_IMAGE_COUNT) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
        }

        long totalSize = 0;
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
            }
            if (!SUPPORTED_IMAGE_TYPES.contains(image.getContentType())) {
                throw new BaseException(ErrorCode.INVALID_FILE_EXTENSION);
            }
            if (image.getSize() > MAX_IMAGE_SIZE) {
                throw new BaseException(ErrorCode.FILE_SIZE_EXCEEDED);
            }
            totalSize += image.getSize();
        }
        if (totalSize > MAX_TOTAL_IMAGE_SIZE) {
            throw new BaseException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        return List.copyOf(images);
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
