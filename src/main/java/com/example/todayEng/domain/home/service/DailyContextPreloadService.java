package com.example.todayEng.domain.home.service;

import com.example.todayEng.domain.diary.client.DiaryContextDataClient;
import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest.Location;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.domain.home.dto.DailyContextPreloadResponse;
import com.example.todayEng.domain.home.dto.DailyContextPreloadResponse.ContextResult;
import com.example.todayEng.domain.home.dto.DailyContextPreloadResponse.ResultStatus;
import com.example.todayEng.domain.home.dto.DailyContextPreloadResponse.SkipReason;
import com.example.todayEng.domain.home.service.DailyContextSnapshotPersistenceService.SnapshotClaim;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.example.todayEng.global.log.ExternalCallLog;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    public DailyContextPreloadResponse preload(Long userId, Location location) {
        LocalDate today = LocalDate.now(clock);
        validateLocation(location);
        if (diaryRepository.findByUserIdAndDiaryDate(userId, today).isPresent()) {
            log.debug("Daily context preload skipped: userId={}, date={}, reason={}",
                    userId, today, SkipReason.DIARY_ALREADY_STARTED);
            return DailyContextPreloadResponse.skipped(today, SkipReason.DIARY_ALREADY_STARTED);
        }

        List<ContextResult> results = new ArrayList<>();
        results.add(location == null
                ? new ContextResult(DiaryContextType.WEATHER, ResultStatus.NO_LOCATION)
                : collect(userId, today, DiaryContextType.WEATHER,
                        () -> contextDataClient.fetchWeather(location, today)));

        externalAccountRepository.findAllByUser_Id(userId).stream()
                .filter(ExternalAccount::isUseEnabled)
                .map(account -> collectExternal(userId, today, account))
                .filter(Objects::nonNull)
                .forEach(results::add);

        return DailyContextPreloadResponse.collected(today, results);
    }

    private ContextResult collectExternal(
            Long userId,
            LocalDate date,
            ExternalAccount account
    ) {
        if (account.getProvider() == ExternalServiceProvider.GOOGLE_CALENDAR) {
            return collect(userId, date, DiaryContextType.CALENDAR,
                    () -> contextDataClient.fetchCalendar(
                            account.getAccessToken(), date));
        } else if (account.getProvider() == ExternalServiceProvider.SPOTIFY) {
            return collect(userId, date, DiaryContextType.SPOTIFY,
                    () -> contextDataClient.fetchSpotify(
                            account.getAccessToken(), date));
        }
        return null;
    }

    private ContextResult collect(
            Long userId,
            LocalDate date,
            DiaryContextType type,
            Supplier<JsonNode> collector
    ) {
        if (persistenceService.findSuccessfulContextData(userId, date, type).isPresent()) {
            return new ContextResult(type, ResultStatus.SUCCEEDED);
        }
        SnapshotClaim claim = claimSnapshot(userId, date, type);
        if (claim == null) {
            log.debug("Daily context preload skipped: userId={}, date={}, type={}, "
                    + "reason=ALREADY_IN_PROGRESS", userId, date, type);
            return new ContextResult(type, ResultStatus.ALREADY_IN_PROGRESS);
        }

        try {
            JsonNode data = collector.get();
            if (data == null) {
                throw new IllegalStateException("Context collector returned no data");
            }
            persistenceService.succeed(claim.id(), claim.leaseVersion(), data);
            return new ContextResult(type, ResultStatus.SUCCEEDED);
        } catch (RuntimeException exception) {
          log.warn(
                  "Daily context preload failed: userId={}, date={}, type={}, cause={}",
                  userId,
                  date,
                  type,
                  ExternalCallLog.describe(exception)
          );
          persistenceService.fail(claim.id(), claim.leaseVersion());
          return new ContextResult(type, ResultStatus.FAILED);
      }
    }

    private SnapshotClaim claimSnapshot(Long userId, LocalDate date, DiaryContextType type) {
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
            throw new BaseException(ErrorCode.INVALID_DIARY_LOCATION);
        }
    }
}
