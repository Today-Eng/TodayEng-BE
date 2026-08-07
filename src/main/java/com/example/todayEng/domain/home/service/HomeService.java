package com.example.todayEng.domain.home.service;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.diary.repository.DiaryAnswerRepository;
import com.example.todayEng.domain.diary.repository.DiaryContextRepository;
import com.example.todayEng.domain.diary.repository.DiaryQuestionRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.home.dto.HomeDiaryDateResponse;
import com.example.todayEng.domain.home.dto.HomeResponse;
import com.example.todayEng.domain.home.dto.HomeResponse.CalendarMaterial;
import com.example.todayEng.domain.home.dto.HomeResponse.RepresentativeEvent;
import com.example.todayEng.domain.home.dto.HomeResponse.SpotifyMaterial;
import com.example.todayEng.domain.home.dto.HomeResponse.TimeMaterial;
import com.example.todayEng.domain.home.dto.HomeResponse.WeatherMaterial;
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
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;
    private final DiaryQuestionRepository diaryQuestionRepository;
    private final DiaryAnswerRepository diaryAnswerRepository;
    private final DiaryContextRepository diaryContextRepository;
    private final DailyContextSnapshotRepository snapshotRepository;
    private final ExternalAccountRepository externalAccountRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public HomeResponse getHome(Long userId, Integer year, Integer month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
        LocalDate today = LocalDate.now(clock);
        YearMonth targetMonth = resolveTargetMonth(year, month, today);
        List<LocalDate> writtenDates = diaryRepository
                .findAllByUserIdAndDiaryDateBetweenOrderByDiaryDate(
                        userId, targetMonth.atDay(1), targetMonth.atEndOfMonth())
                .stream()
                .filter(d -> d.getStatus() != DiaryStatus.DELETED)
                .map(Diary::getDiaryDate)
                .toList();
        Optional<Diary> todayDiary = diaryRepository.findByUserIdAndDiaryDate(userId, today);
        Map<DiaryContextType, JsonNode> contexts =
                loadSuccessfulContexts(userId, today, todayDiary);
        Map<ExternalServiceProvider, ExternalAccount> accounts = externalAccountRepository
                .findAllByUser_Id(userId)
                .stream()
                .collect(Collectors.toMap(
                        ExternalAccount::getProvider,
                        Function.identity(),
                        (first, ignored) -> first,
                        () -> new EnumMap<>(ExternalServiceProvider.class)
                ));

        return new HomeResponse(
                new HomeResponse.UserSummary(user.getNickname()),
                new HomeResponse.Statistics(
                        user.getTotalDiaryCount(),
                        user.getCurrentStreak()
                ),
                new HomeResponse.CalendarSummary(
                        targetMonth.getYear(),
                        targetMonth.getMonthValue(),
                        writtenDates
                ),
                createToday(today, todayDiary),
                new HomeResponse.Materials(
                        createTimeMaterial(LocalTime.now(clock)),
                        createWeather(contexts.get(DiaryContextType.WEATHER)),
                        createCalendar(
                                accounts.get(ExternalServiceProvider.GOOGLE_CALENDAR),
                                contexts.get(DiaryContextType.CALENDAR)
                        ),
                        createSpotify(
                                accounts.get(ExternalServiceProvider.SPOTIFY),
                                contexts.get(DiaryContextType.SPOTIFY)
                        )
                )
        );
    }

    @Transactional(readOnly = true)
    public HomeDiaryDateResponse getDiaryByDate(Long userId, LocalDate date) {
        Optional<Diary> found = diaryRepository.findByUserIdAndDiaryDate(userId, date);

        if (found.isEmpty()) {
            return new HomeDiaryDateResponse(
                    null,
                    date,
                    date.getDayOfWeek().name(),
                    "NOT_STARTED",
                    List.of(),
                    null,
                    null
            );
        }

        Diary diary = found.get();

        if (diary.getStatus() == DiaryStatus.DELETED) {
            return new HomeDiaryDateResponse(
                    diary.getId(),
                    diary.getDiaryDate(),
                    diary.getDiaryDate().getDayOfWeek().name(),
                    "UNAVAILABLE",
                    List.of(),
                    null,
                    null
            );
        }

        String diaryStatus = diary.getStatus() == DiaryStatus.COMPLETED
                ? "COMPLETED"
                : "IN_PROGRESS";

        if (diary.getStatus() != DiaryStatus.COMPLETED) {
            return new HomeDiaryDateResponse(
                    diary.getId(),
                    diary.getDiaryDate(),
                    diary.getDiaryDate().getDayOfWeek().name(),
                    diaryStatus,
                    List.of(),
                    null,
                    null
            );
        }

        List<DiaryQuestion> mainQuestions = diaryQuestionRepository
                .findAllByDiaryIdInAndQuestionTypeOrderByDiaryIdAscQuestionOrderAsc(
                        List.of(diary.getId()),
                        QuestionType.MAIN
                );

        List<String> keywords = mainQuestions.stream()
                .map(DiaryQuestion::getKeyword)
                .filter(Objects::nonNull)
                .filter(keyword -> !keyword.isBlank())
                .toList();

        DiaryQuestion firstQuestion = mainQuestions.isEmpty() ? null : mainQuestions.get(0);
        String questionText = firstQuestion == null ? null : firstQuestion.getQuestionText();

        String correctedText = null;
        if (firstQuestion != null) {
            List<DiaryAnswer> answers = diaryAnswerRepository
                    .findAllByQuestionIdIn(List.of(firstQuestion.getId()));
            if (!answers.isEmpty()) {
                DiaryAnswer answer = answers.get(0);
                String ct = answer.getCorrectedText();
                correctedText = (ct != null && !ct.isBlank()) ? ct : answer.getOriginalText();
            }
        }

        return new HomeDiaryDateResponse(
                diary.getId(),
                diary.getDiaryDate(),
                diary.getDiaryDate().getDayOfWeek().name(),
                diaryStatus,
                keywords,
                questionText,
                correctedText
        );
    }

    private YearMonth resolveTargetMonth(Integer year, Integer month, LocalDate today) {
        int resolvedYear = year == null ? today.getYear() : year;
        int resolvedMonth = month == null ? today.getMonthValue() : month;
        if (resolvedYear < 1 || resolvedYear > 9999
                || resolvedMonth < 1 || resolvedMonth > 12) {
            throw new BaseException(ErrorCode.INVALID_CALENDAR_DATE);
        }
        try {
            return YearMonth.of(resolvedYear, resolvedMonth);
        } catch (DateTimeException exception) {
            throw new BaseException(ErrorCode.INVALID_CALENDAR_DATE);
        }
    }

    private Map<DiaryContextType, JsonNode> loadSuccessfulContexts(
            Long userId,
            LocalDate today,
            Optional<Diary> diary
    ) {
        Map<DiaryContextType, JsonNode> contexts = snapshotRepository
                .findAllByUserIdAndContextDateAndCollectionStatus(
                        userId,
                        today,
                        DailyContextCollectionStatus.SUCCEEDED
                )
                .stream()
                .collect(Collectors.toMap(
                        DailyContextSnapshot::getContextType,
                        DailyContextSnapshot::getContextData,
                        (first, ignored) -> first,
                        () -> new EnumMap<>(DiaryContextType.class)
                ));
        diary.ifPresent(value -> diaryContextRepository
                .findAllByDiaryIdAndSuccessTrueOrderById(value.getId())
                .forEach(context -> contexts.put(
                        context.getContextType(),
                        context.getContextData()
                )));
        return contexts;
    }

    private HomeResponse.TodaySummary createToday(
            LocalDate today,
            Optional<Diary> diary
    ) {
        String status = diary
                .map(Diary::getStatus)
                .map(value -> {
                    if (value == DiaryStatus.COMPLETED) {
                        return "COMPLETED";
                    }
                    if (value == DiaryStatus.DELETED) {
                        return "UNAVAILABLE";
                    }
                    return "IN_PROGRESS";
                })
                .orElse("NOT_STARTED");
        return new HomeResponse.TodaySummary(
                today,
                today.getDayOfWeek().name(),
                status,
                diary.map(Diary::getId).orElse(null)
        );
    }

    private TimeMaterial createTimeMaterial(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 5 && hour < 12) {
            return new TimeMaterial("MORNING", "아침입니다");
        }
        if (hour >= 12 && hour < 18) {
            return new TimeMaterial("AFTERNOON", "오후입니다");
        }
        if (hour >= 18 && hour < 22) {
            return new TimeMaterial("EVENING", "저녁입니다");
        }
        return new TimeMaterial("NIGHT", "밤입니다");
    }

    private WeatherMaterial createWeather(JsonNode contextData) {
        if (contextData == null) {
            return new WeatherMaterial(false, null, null);
        }
        JsonNode daily = contextData.path("daily");
        Integer weatherCode = firstInt(daily.path("weather_code"));
        Double maximum = firstDouble(daily.path("temperature_2m_max"));
        Double minimum = firstDouble(daily.path("temperature_2m_min"));
        if (weatherCode == null || (maximum == null && minimum == null)) {
            return new WeatherMaterial(false, null, null);
        }
        double temperature = maximum == null
                ? minimum
                : minimum == null ? maximum : (maximum + minimum) / 2;
        return new WeatherMaterial(
                true,
                weatherCondition(weatherCode),
                (int) Math.round(temperature)
        );
    }

    private CalendarMaterial createCalendar(
            ExternalAccount account,
            JsonNode contextData
    ) {
        boolean connected = account != null;
        boolean enabled = connected && account.isUseEnabled();
        if (!enabled || contextData == null) {
            return new CalendarMaterial(connected, enabled, 0, null);
        }
        JsonNode items = contextData.path("items");
        if (!items.isArray()) {
            return new CalendarMaterial(connected, enabled, 0, null);
        }
        RepresentativeEvent event = items.isEmpty() ? null : parseEvent(items.get(0));
        return new CalendarMaterial(connected, enabled, items.size(), event);
    }

    private SpotifyMaterial createSpotify(
            ExternalAccount account,
            JsonNode contextData
    ) {
        boolean connected = account != null;
        boolean enabled = connected && account.isUseEnabled();
        if (!enabled || contextData == null) {
            return new SpotifyMaterial(connected, enabled, false, null, null);
        }
        JsonNode items = contextData.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return new SpotifyMaterial(connected, enabled, false, null, null);
        }
        JsonNode track = items.get(0).path("track");
        String title = textOrNull(track.path("name"));
        JsonNode artists = track.path("artists");
        String artist = artists.isArray() && !artists.isEmpty()
                ? textOrNull(artists.get(0).path("name"))
                : null;
        boolean available = title != null;
        return new SpotifyMaterial(
                connected,
                enabled,
                available,
                available ? title : null,
                available ? artist : null
        );
    }

    private RepresentativeEvent parseEvent(JsonNode item) {
        String title = textOrNull(item.path("summary"));
        String rawStart = textOrNull(item.path("start").path("dateTime"));
        if (rawStart == null) {
            String date = textOrNull(item.path("start").path("date"));
            try {
                return new RepresentativeEvent(
                        title,
                        date == null ? null : LocalDate.parse(date).atStartOfDay()
                );
            } catch (DateTimeException exception) {
                return new RepresentativeEvent(title, null);
            }
        }
        try {
            return new RepresentativeEvent(
                    title,
                    java.time.OffsetDateTime.parse(rawStart).toLocalDateTime()
            );
        } catch (DateTimeException exception) {
            try {
                return new RepresentativeEvent(title, LocalDateTime.parse(rawStart));
            } catch (DateTimeException ignored) {
                return new RepresentativeEvent(title, null);
            }
        }
    }

    private Integer firstInt(JsonNode node) {
        return node.isArray() && !node.isEmpty() && node.get(0).isNumber()
                ? node.get(0).intValue()
                : null;
    }

    private Double firstDouble(JsonNode node) {
        return node.isArray() && !node.isEmpty() && node.get(0).isNumber()
                ? node.get(0).doubleValue()
                : null;
    }

    private String textOrNull(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
    }

    private String weatherCondition(int code) {
        if (code == 0) return "CLEAR";
        if (code <= 3) return "CLOUDY";
        if (code == 45 || code == 48) return "FOG";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "RAIN";
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return "SNOW";
        if (code >= 95) return "THUNDERSTORM";
        return "UNKNOWN";
    }
}
