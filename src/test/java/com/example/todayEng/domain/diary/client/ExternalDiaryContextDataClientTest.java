package com.example.todayEng.domain.diary.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ExternalDiaryContextDataClientTest {

    private MockRestServiceServer server;
    private ExternalDiaryContextDataClient client;
    private String requestedUri;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ExternalDiaryContextDataClient(builder.build());
        server.expect(request -> requestedUri = request.getURI().toString())
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("캘린더 조회 구간은 초를 포함한 RFC3339 형식이어야 한다")
    void sendsRfc3339TimeRangeWithSeconds() {
        client.fetchCalendar("token", LocalDate.of(2026, 8, 10));

        assertThat(requestedUri)
                .contains("timeMin=2026-08-09T15:00:00Z")
                .contains("timeMax=2026-08-10T15:00:00Z");
    }

    @Test
    @DisplayName("구간 파라미터에 인코딩되지 않은 +가 있으면 서버가 공백으로 해석한다")
    void doesNotSendRawPlusInQuery() {
        client.fetchCalendar("token", LocalDate.of(2026, 8, 10));

        assertThat(requestedUri.substring(requestedUri.indexOf('?')))
                .doesNotContain("+");
    }

    @Test
    @DisplayName("스포티파이 조회는 해당 날짜 시작 시각의 밀리초 epoch를 사용한다")
    void sendsSpotifyEpochMillis() {
        client.fetchSpotify("token", LocalDate.of(2026, 8, 10));

        assertThat(requestedUri).contains("after=1786287600000");
    }
}
