package com.example.todayEng.domain.diary.client;

import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest.Location;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class ExternalDiaryContextDataClient implements DiaryContextDataClient {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private final RestClient restClient;

    @Override
    public JsonNode fetchCalendar(String accessToken, LocalDate date) {
        String uri = UriComponentsBuilder
                .fromUriString("https://www.googleapis.com/calendar/v3/calendars/primary/events")
                .queryParam("timeMin", rfc3339(date))
                .queryParam("timeMax", rfc3339(date.plusDays(1)))
                .queryParam("singleEvents", true)
                .queryParam("orderBy", "startTime")
                .build().encode().toUriString();
        return getWithBearer(uri, accessToken);
    }

    private String rfc3339(LocalDate date) {
        return date.atStartOfDay(SERVICE_ZONE).toInstant().toString();
    }

    @Override
    public JsonNode fetchSpotify(String accessToken, LocalDate date) {
        long after = date.atStartOfDay(SERVICE_ZONE).toInstant().toEpochMilli();
        String uri = UriComponentsBuilder
                .fromUriString("https://api.spotify.com/v1/me/player/recently-played")
                .queryParam("after", after)
                .queryParam("limit", 50)
                .build().encode().toUriString();
        return getWithBearer(uri, accessToken);
    }

    @Override
    public JsonNode fetchWeather(Location location, LocalDate date) {
        String uri = UriComponentsBuilder
                .fromUriString("https://archive-api.open-meteo.com/v1/archive")
                .queryParam("latitude", location.latitude())
                .queryParam("longitude", location.longitude())
                .queryParam("start_date", date)
                .queryParam("end_date", date)
                .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum")
                .queryParam("timezone", "Asia/Seoul")
                .build().encode().toUriString();
        return restClient.get().uri(uri).retrieve().body(JsonNode.class);
    }

    private JsonNode getWithBearer(String uri, String accessToken) {
        return restClient.get().uri(uri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve().body(JsonNode.class);
    }
}
