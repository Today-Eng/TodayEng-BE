package com.example.todayEng.domain.home.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HomeResponse(
        UserSummary user,
        Statistics statistics,
        CalendarSummary calendar,
        TodaySummary today,
        Materials materials
) {
    public record UserSummary(String nickname) {}

    public record Statistics(long totalDiaryCount, int currentDiaryStreak) {}

    public record CalendarSummary(int year, int month, List<LocalDate> writtenDates) {}

    public record TodaySummary(
            LocalDate date,
            String dayOfWeek,
            String diaryStatus,
            Long diaryId
    ) {}

    public record Materials(
            TimeMaterial time,
            WeatherMaterial weather,
            CalendarMaterial calendar,
            SpotifyMaterial spotify
    ) {}

    public record TimeMaterial(String period, String message) {}

    public record WeatherMaterial(boolean available, String condition, Integer temperature) {}

    public record CalendarMaterial(
            boolean connected,
            boolean useEnabled,
            int eventCount,
            RepresentativeEvent representativeEvent
    ) {}

    public record RepresentativeEvent(String title, LocalDateTime startAt) {}

    public record SpotifyMaterial(
            boolean connected,
            boolean useEnabled,
            boolean recentTrackAvailable,
            String trackTitle,
            String artistName
    ) {}
}
