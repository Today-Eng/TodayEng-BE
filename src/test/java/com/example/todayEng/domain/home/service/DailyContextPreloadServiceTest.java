package com.example.todayEng.domain.home.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.diary.client.DiaryContextDataClient;
import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest.Location;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
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
    @Mock ExternalAccount account;

    DailyContextPreloadService service;

    @BeforeEach
    void setUp() {
        service = new DailyContextPreloadService(
                accountRepository,
                diaryRepository,
                dataClient,
                persistenceService,
                Clock.fixed(
                        Instant.parse("2026-08-06T00:00:00Z"),
                        ZoneId.of("Asia/Seoul")
                )
        );
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
                .willReturn(10L);
        given(persistenceService.start(USER_ID, TODAY, DiaryContextType.CALENDAR))
                .willReturn(11L);
        given(dataClient.fetchWeather(location, TODAY))
                .willReturn(new ObjectMapper().createObjectNode());
        given(dataClient.fetchCalendar("token", TODAY))
                .willReturn(new ObjectMapper().createObjectNode());

        service.preload(USER_ID, location);

        verify(persistenceService).succeed(10L,
                new ObjectMapper().createObjectNode());
        verify(persistenceService).succeed(11L,
                new ObjectMapper().createObjectNode());
    }

    @Test
    void skipsExternalCallWhenSnapshotWasAlreadyClaimed() {
        given(persistenceService.start(USER_ID, TODAY, DiaryContextType.WEATHER))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        service.preload(USER_ID, new Location(37.5, 127.0));

        verify(dataClient, never()).fetchWeather(any(), any());
    }

    @Test
    void recordsFailureWithoutThrowingWhenExternalCallFails() {
        given(persistenceService.start(USER_ID, TODAY, DiaryContextType.WEATHER))
                .willReturn(10L);
        given(dataClient.fetchWeather(any(), any()))
                .willThrow(new RuntimeException("timeout"));

        service.preload(USER_ID, new Location(37.5, 127.0));

        verify(persistenceService).fail(10L);
    }

    @Test
    void skipsPreloadWhenTodayDiaryAlreadyExists() {
        given(diaryRepository.findByUserIdAndDiaryDate(USER_ID, TODAY))
                .willReturn(Optional.of(org.mockito.Mockito.mock(Diary.class)));

        service.preload(USER_ID, new Location(37.5, 127.0));

        verify(persistenceService, never()).start(any(), any(), any());
        verify(dataClient, never()).fetchWeather(any(), any());
    }
}
