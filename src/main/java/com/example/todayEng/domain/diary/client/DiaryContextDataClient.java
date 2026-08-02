package com.example.todayEng.domain.diary.client;

import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest.Location;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;

public interface DiaryContextDataClient {

    JsonNode fetchCalendar(String accessToken, LocalDate date);

    JsonNode fetchSpotify(String accessToken, LocalDate date);

    JsonNode fetchWeather(Location location, LocalDate date);
}
