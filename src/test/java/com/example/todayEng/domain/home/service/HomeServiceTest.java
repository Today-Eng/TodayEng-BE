package com.example.todayEng.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.repository.DiaryContextRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.home.entity.DailyContextSnapshot;
import com.example.todayEng.domain.home.entity.enums.DailyContextCollectionStatus;
import com.example.todayEng.domain.home.repository.DailyContextSnapshotRepository;
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.domain.user.repository.UserRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HomeServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 24);

    @Mock UserRepository userRepository;
    @Mock DiaryRepository diaryRepository;
    @Mock DiaryContextRepository diaryContextRepository;
    @Mock DailyContextSnapshotRepository snapshotRepository;
    @Mock ExternalAccountRepository externalAccountRepository;

    HomeService homeService;
    User user;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-24T10:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        homeService = new HomeService(
                userRepository,
                diaryRepository,
                diaryContextRepository,
                snapshotRepository,
                externalAccountRepository,
                clock
        );
        user = User.create();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        ReflectionTestUtils.setField(user, "nickname", "은우");
        ReflectionTestUtils.setField(user, "totalDiaryCount", 12L);
        ReflectionTestUtils.setField(user, "currentStreak", 4);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(diaryRepository.findAllByUserIdAndDiaryDateBetweenOrderByDiaryDate(
                any(), any(), any())).willReturn(List.of());
        given(diaryRepository.findByUserIdAndDiaryDate(USER_ID, TODAY))
                .willReturn(Optional.empty());
        given(externalAccountRepository.findAllByUser_Id(USER_ID)).willReturn(List.of());
        given(snapshotRepository.findAllByUserIdAndContextDateAndCollectionStatus(
                USER_ID, TODAY, DailyContextCollectionStatus.SUCCEEDED
        )).willReturn(List.of());
    }

    @Test
    void returnsCurrentMonthAndNotStartedTodayByDefault() {
        var response = homeService.getHome(USER_ID, null, null);

        assertThat(response.user().nickname()).isEqualTo("은우");
        assertThat(response.statistics().totalDiaryCount()).isEqualTo(12);
        assertThat(response.calendar().year()).isEqualTo(2026);
        assertThat(response.calendar().month()).isEqualTo(7);
        assertThat(response.today().date()).isEqualTo(TODAY);
        assertThat(response.today().dayOfWeek()).isEqualTo("FRIDAY");
        assertThat(response.today().diaryStatus()).isEqualTo("NOT_STARTED");
        assertThat(response.materials().time().period()).isEqualTo("EVENING");
        assertThat(response.materials().weather().available()).isFalse();
        assertThat(response.materials().calendar().connected()).isFalse();
        assertThat(response.materials().spotify().connected()).isFalse();
    }

    @Test
    void mapsStoredContextsAndPausedDiaryToHomeContract() throws Exception {
        Diary diary = Diary.create(user, TODAY);
        ReflectionTestUtils.setField(diary, "id", 10L);
        diary.pause(TODAY.atTime(10, 0), TODAY.plusDays(1).atStartOfDay());
        given(diaryRepository.findByUserIdAndDiaryDate(USER_ID, TODAY))
                .willReturn(Optional.of(diary));

        ObjectMapper objectMapper = new ObjectMapper();
        DiaryContext weather = DiaryContext.success(
                diary,
                DiaryContextType.WEATHER,
                objectMapper.readTree("""
                        {"daily":{"weather_code":[2],
                        "temperature_2m_max":[24.0],"temperature_2m_min":[18.0]}}
                        """)
        );
        DiaryContext calendar = DiaryContext.success(
                diary,
                DiaryContextType.CALENDAR,
                objectMapper.readTree("""
                        {"items":[{"summary":"친구 약속",
                        "start":{"dateTime":"2026-07-24T19:00:00+09:00"}}]}
                        """)
        );
        DiaryContext spotify = DiaryContext.success(
                diary,
                DiaryContextType.SPOTIFY,
                objectMapper.readTree("""
                        {"items":[{"track":{"name":"Good Days",
                        "artists":[{"name":"Example Artist"}]}}]}
                        """)
        );
        given(diaryContextRepository.findAllByDiaryIdAndSuccessTrueOrderById(10L))
                .willReturn(List.of(weather, calendar, spotify));

        ExternalAccount google = mock(ExternalAccount.class);
        given(google.getProvider()).willReturn(ExternalServiceProvider.GOOGLE_CALENDAR);
        given(google.isUseEnabled()).willReturn(true);
        ExternalAccount spotifyAccount = mock(ExternalAccount.class);
        given(spotifyAccount.getProvider()).willReturn(ExternalServiceProvider.SPOTIFY);
        given(spotifyAccount.isUseEnabled()).willReturn(true);
        given(externalAccountRepository.findAllByUser_Id(USER_ID))
                .willReturn(List.of(google, spotifyAccount));

        var response = homeService.getHome(USER_ID, 2026, 7);

        assertThat(response.today().diaryStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.today().diaryId()).isEqualTo(10L);
        assertThat(response.materials().weather().condition()).isEqualTo("CLOUDY");
        assertThat(response.materials().weather().temperature()).isEqualTo(21);
        assertThat(response.materials().calendar().eventCount()).isEqualTo(1);
        assertThat(response.materials().calendar().representativeEvent().title())
                .isEqualTo("친구 약속");
        assertThat(response.materials().spotify().recentTrackAvailable()).isTrue();
        assertThat(response.materials().spotify().trackTitle()).isEqualTo("Good Days");
    }

    @Test
    void rejectsInvalidCalendarMonth() {
        assertThatThrownBy(() -> homeService.getHome(USER_ID, 2026, 13))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CALENDAR_DATE);
    }

    @Test
    void usesSuccessfulSnapshotBeforeDiaryIsCreated() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DailyContextSnapshot snapshot = DailyContextSnapshot.start(
                user, TODAY, DiaryContextType.WEATHER);
        snapshot.succeed(objectMapper.readTree("""
                {"daily":{"weather_code":[0],
                "temperature_2m_max":[22.0],"temperature_2m_min":[18.0]}}
                """));
        given(snapshotRepository.findAllByUserIdAndContextDateAndCollectionStatus(
                USER_ID, TODAY, DailyContextCollectionStatus.SUCCEEDED
        )).willReturn(List.of(snapshot));

        var response = homeService.getHome(USER_ID, null, null);

        assertThat(response.today().diaryStatus()).isEqualTo("NOT_STARTED");
        assertThat(response.materials().weather().available()).isTrue();
        assertThat(response.materials().weather().condition()).isEqualTo("CLEAR");
    }

    @Test
    void prefersDiaryContextOverSnapshotForSameType() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DailyContextSnapshot snapshot = DailyContextSnapshot.start(
                user, TODAY, DiaryContextType.WEATHER);
        snapshot.succeed(objectMapper.readTree("""
                {"daily":{"weather_code":[0],
                "temperature_2m_max":[22.0],"temperature_2m_min":[18.0]}}
                """));
        given(snapshotRepository.findAllByUserIdAndContextDateAndCollectionStatus(
                USER_ID, TODAY, DailyContextCollectionStatus.SUCCEEDED
        )).willReturn(List.of(snapshot));

        Diary diary = Diary.create(user, TODAY);
        ReflectionTestUtils.setField(diary, "id", 10L);
        given(diaryRepository.findByUserIdAndDiaryDate(USER_ID, TODAY))
                .willReturn(Optional.of(diary));
        DiaryContext weather = DiaryContext.success(
                diary,
                DiaryContextType.WEATHER,
                objectMapper.readTree("""
                        {"daily":{"weather_code":[95],
                        "temperature_2m_max":[30.0],"temperature_2m_min":[26.0]}}
                        """)
        );
        given(diaryContextRepository.findAllByDiaryIdAndSuccessTrueOrderById(10L))
                .willReturn(List.of(weather));

        var response = homeService.getHome(USER_ID, null, null);

        assertThat(response.materials().weather().condition()).isEqualTo("THUNDERSTORM");
        assertThat(response.materials().weather().temperature()).isEqualTo(28);
    }
}
