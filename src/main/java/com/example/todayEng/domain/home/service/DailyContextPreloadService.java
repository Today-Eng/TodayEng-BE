package com.example.todayEng.domain.home.service;

import com.example.todayEng.domain.diary.client.DiaryContextDataClient;
import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest.Location;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyContextPreloadService {

    private final ExternalAccountRepository externalAccountRepository;
    private final DiaryRepository diaryRepository;
    private final DiaryContextDataClient contextDataClient;
    private final DailyContextSnapshotPersistenceService persistenceService;
    private final Clock clock;

    public void preload(Long userId, Location location) {
        LocalDate today = LocalDate.now(clock);
        validateLocation(location);
        if (diaryRepository.findByUserIdAndDiaryDate(userId, today).isPresent()) {
            return;
        }
        if (location != null) {
            collect(userId, today, DiaryContextType.WEATHER,
                    () -> contextDataClient.fetchWeather(location, today));
        }

        externalAccountRepository.findAllByUser_Id(userId).stream()
                .filter(ExternalAccount::isUseEnabled)
                .forEach(account -> collectExternal(userId, today, account));
    }

    private void collectExternal(
            Long userId,
            LocalDate date,
            ExternalAccount account
    ) {
        if (account.getProvider() == ExternalServiceProvider.GOOGLE_CALENDAR) {
            collect(userId, date, DiaryContextType.CALENDAR,
                    () -> contextDataClient.fetchCalendar(
                            account.getAccessToken(), date));
        } else if (account.getProvider() == ExternalServiceProvider.SPOTIFY) {
            collect(userId, date, DiaryContextType.SPOTIFY,
                    () -> contextDataClient.fetchSpotify(
                            account.getAccessToken(), date));
        }
    }

    private void collect(
            Long userId,
            LocalDate date,
            DiaryContextType type,
            Supplier<JsonNode> collector
    ) {
        Long snapshotId = claimSnapshot(userId, date, type);
        if (snapshotId == null) {
            return;
        }

        try {
            JsonNode data = collector.get();
            if (data == null) {
                throw new IllegalStateException("Context collector returned no data");
            }
            persistenceService.succeed(snapshotId, data);
        } catch (RuntimeException exception) {
            log.warn("Daily context preload failed: type={}, exception={}",
                    type, exception.getClass().getSimpleName());
            persistenceService.fail(snapshotId);
        }
    }

    private Long claimSnapshot(Long userId, LocalDate date, DiaryContextType type) {
        try {
            return persistenceService.start(userId, date, type);
        } catch (DataIntegrityViolationException exception) {
            return persistenceService.reclaimStale(userId, date, type).orElse(null);
        }
    }

    private void validateLocation(Location location) {
        if (location != null
                && (!Double.isFinite(location.latitude())
                || !Double.isFinite(location.longitude())
                || location.latitude() < -90 || location.latitude() > 90
                || location.longitude() < -180 || location.longitude() > 180)) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
