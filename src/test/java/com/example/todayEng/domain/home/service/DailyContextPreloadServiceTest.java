package com.example.todayEng.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.diary.client.DiaryContextDataClient;
import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest.Location;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.home.dto.DailyContextPreloadResponse;
import com.example.todayEng.domain.home.dto.DailyContextPreloadResponse.ContextResult;
import com.example.todayEng.domain.home.dto.DailyContextPreloadResponse.ResultStatus;
import com.example.todayEng.domain.home.dto.DailyContextPreloadResponse.SkipReason;
import com.example.todayEng.domain.home.service.DailyContextSnapshotPersistenceService.SnapshotClaim;
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.domain.user.service.ExternalAccountTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class DailyContextPreloadServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 6);

    @Mock ExternalAccountRepository accountRepository;
    @Mock DiaryRepository diaryRepository;
    @Mock DiaryContextDataClient dataClient;
    @Mock DailyContextSnapshotPersistenceService persistenceService;
    @Mock ExternalAccountTokenService tokenService;
    @Mock ExternalAccount account;

    DailyContextPreloadService service;

    @BeforeEach
    void setUp() {
        service = new DailyContextPreloadService(
                accountRepository,
                diaryRepository,
                dataClient,
                persistenceService,
                tokenService,
                Clock.fixed(
                        Instant.parse("2026-08-06T00:00:00Z"),
                        ZoneId.of("Asia/Seoul")
                )
        );
        lenient().when(tokenService.<JsonNode>callWithAccessToken(any(), any()))
                .thenAnswer(invocation -> {
                    ExternalAccount target = invocation.getArgument(0);
                    Function<String, JsonNode> call = invocation.getArgument(1);
                    return call.apply(target.getAccessToken());
                });
        given(diaryRepository.findByUserIdAndDiaryDate(USER_ID, TODAY))
                .willReturn(Optional.empty());
        lenient().when(accountRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of());
    }

    @Test
    void collectsEnabledExternalAccountAndWeatherIndependently() {
        Location location = new Location(37.5, 127.0);
        given(account.isUseEnabled()).willReturn(true);
        given(account.getProvider()).willReturn(ExternalServiceProvider.GOOGLE_CALENDAR);
        given(account.getAccessToken()).willReturn("token");
        given(accountRepository.findAllByUser_Id(USER_ID)).willReturn(List.of(account));
        given(persistenceService.start(USER_ID, TODAY, DiaryContextType.WEATHER))
                .willReturn(new SnapshotClaim(10L, 0L));
        given(persistenceService.start(USER_ID, TODAY, DiaryContextType.CALENDAR))
                .willReturn(new SnapshotClaim(11L, 0L));
        given(dataClient.fetchWeather(location, TODAY))
                .willReturn(new ObjectMapper().createObjectNode());
        given(dataClient.fetchCalendar("token", TODAY))
                .willReturn(new ObjectMapper().createObjectNode());

        DailyContextPreloadResponse response = service.preload(USER_ID, location);

        verify(persistenceService).succeed(10L, 0L,
                new ObjectMapper().createObjectNode());
        verify(persistenceService).succeed(11L, 0L,
                new ObjectMapper().createObjectNode());
        assertThat(response.contextDate()).isEqualTo(TODAY);
        assertThat(response.skipReason()).isNull();
        assertThat(response.contexts())
                .extracting(ContextResult::type, ContextResult::status)
                .containsExactly(
                        tuple(DiaryContextType.WEATHER, ResultStatus.SUCCEEDED),
                        tuple(DiaryContextType.CALENDAR, ResultStatus.SUCCEEDED)
                );
    }

    @Test
    void reportsNoLocationWhenCoordinatesAreAbsent() {
        DailyContextPreloadResponse response = service.preload(USER_ID, null);

        verify(persistenceService, never()).start(any(), any(), any());
        assertThat(response.contexts())
                .extracting(ContextResult::type, ContextResult::status)
                .containsExactly(tuple(DiaryContextType.WEATHER, ResultStatus.NO_LOCATION));
    }

    @Test
    void reusesSuccessfulSnapshotWithoutCallingExternalApi() {
        given(persistenceService.findSuccessfulContextData(
                USER_ID, TODAY, DiaryContextType.WEATHER))
                .willReturn(Optional.of(new ObjectMapper().createObjectNode()));

        DailyContextPreloadResponse response =
                service.preload(USER_ID, new Location(37.5, 127.0));

        verify(persistenceService, never()).start(any(), any(), any());
        verify(dataClient, never()).fetchWeather(any(), any());
        assertThat(response.contexts())
                .extracting(ContextResult::type, ContextResult::status)
                .containsExactly(tuple(DiaryContextType.WEATHER, ResultStatus.SUCCEEDED));
    }

    @Test
    void skipsExternalCallWhenActiveSnapshotCannotBeReclaimed() {
        given(persistenceService.start(USER_ID, TODAY, DiaryContextType.WEATHER))
                .willThrow(new DataIntegrityViolationException("duplicate"));
        given(persistenceService.reclaimStale(USER_ID, TODAY, DiaryContextType.WEATHER))
                .willReturn(Optional.empty());

        DailyContextPreloadResponse response = service.preload(USER_ID, new Location(37.5, 127.0));

        verify(dataClient, never()).fetchWeather(any(), any());
        verify(persistenceService, never()).succeed(any(), anyLong(), any());
        verify(persistenceService, never()).fail(any(), anyLong());
        assertThat(response.contexts())
                .extracting(ContextResult::type, ContextResult::status)
                .containsExactly(
                        tuple(DiaryContextType.WEATHER, ResultStatus.ALREADY_IN_PROGRESS));
    }

    @Test
    void retriesContextAfterStaleInProgressSnapshotIsReclaimed() {
        given(persistenceService.start(USER_ID, TODAY, DiaryContextType.WEATHER))
                .willThrow(new DataIntegrityViolationException("duplicate"));
        given(persistenceService.reclaimStale(USER_ID, TODAY, DiaryContextType.WEATHER))
                .willReturn(Optional.of(new SnapshotClaim(10L, 1L)));
        given(dataClient.fetchWeather(any(), any()))
                .willReturn(new ObjectMapper().createObjectNode());

        service.preload(USER_ID, new Location(37.5, 127.0));

        verify(persistenceService).succeed(10L, 1L, new ObjectMapper().createObjectNode());
    }

    @Test
    void recordsFailureWithoutThrowingWhenExternalCallFails() {
        given(persistenceService.start(USER_ID, TODAY, DiaryContextType.WEATHER))
                .willReturn(new SnapshotClaim(10L, 0L));
        given(dataClient.fetchWeather(any(), any()))
                .willThrow(new RuntimeException("timeout"));

        DailyContextPreloadResponse response = service.preload(USER_ID, new Location(37.5, 127.0));

        verify(persistenceService).fail(10L, 0L);
        assertThat(response.contexts())
                .extracting(ContextResult::type, ContextResult::status)
                .containsExactly(tuple(DiaryContextType.WEATHER, ResultStatus.FAILED));
    }

    @Test
    void collectsCalendarSuccessfullyWhenWeatherFails() {
        Location location = new Location(37.5, 127.0);
        given(account.isUseEnabled()).willReturn(true);
        given(account.getProvider()).willReturn(ExternalServiceProvider.GOOGLE_CALENDAR);
        given(account.getAccessToken()).willReturn("token");
        given(accountRepository.findAllByUser_Id(USER_ID)).willReturn(List.of(account));
        given(persistenceService.start(USER_ID, TODAY, DiaryContextType.WEATHER))
                .willReturn(new SnapshotClaim(10L, 0L));
        given(persistenceService.start(USER_ID, TODAY, DiaryContextType.CALENDAR))
                .willReturn(new SnapshotClaim(11L, 0L));
        given(dataClient.fetchWeather(location, TODAY))
                .willThrow(new RuntimeException("timeout"));
        given(dataClient.fetchCalendar("token", TODAY))
                .willReturn(new ObjectMapper().createObjectNode());

        service.preload(USER_ID, location);

        verify(persistenceService).fail(10L, 0L);
        verify(persistenceService).succeed(11L, 0L, new ObjectMapper().createObjectNode());
    }

    @Test
    void skipsPreloadWhenTodayDiaryAlreadyExists() {
        given(diaryRepository.findByUserIdAndDiaryDate(USER_ID, TODAY))
                .willReturn(Optional.of(org.mockito.Mockito.mock(Diary.class)));

        DailyContextPreloadResponse response = service.preload(USER_ID, new Location(37.5, 127.0));

        verify(persistenceService, never()).start(any(), any(), any());
        verify(dataClient, never()).fetchWeather(any(), any());
        assertThat(response.skipReason()).isEqualTo(SkipReason.DIARY_ALREADY_STARTED);
        assertThat(response.contexts()).isEmpty();
    }
}
