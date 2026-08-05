package com.example.todayEng.domain.diary.client;

import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest.Location;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/** Uses the forecast endpoint because diaries can target today through six days ago. */
@Primary
@Component
public class RecentWeatherDiaryContextDataClient extends ExternalDiaryContextDataClient {

    private final RestClient restClient;

    public RecentWeatherDiaryContextDataClient(RestClient restClient) {
        super(restClient);
        this.restClient = restClient;
    }

    @Override
    public JsonNode fetchWeather(Location location, LocalDate date) {
        String uri = UriComponentsBuilder
                .fromUriString("https://api.open-meteo.com/v1/forecast")
                .queryParam("latitude", location.latitude())
                .queryParam("longitude", location.longitude())
                .queryParam("start_date", date)
                .queryParam("end_date", date)
                .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum")
                .queryParam("timezone", "Asia/Seoul")
                .build().encode().toUriString();
        return restClient.get().uri(uri).retrieve().body(JsonNode.class);
    }
}
