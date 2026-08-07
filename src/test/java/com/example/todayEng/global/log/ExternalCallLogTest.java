package com.example.todayEng.global.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

class ExternalCallLogTest {

    @Test
    @DisplayName("HTTP 오류는 상태 코드만 남기고 응답 본문은 남기지 않는다")
    void describesHttpErrorWithoutResponseBody() {
        String sensitiveBody = "{\"summary\":\"공원에서 친구와 점심\"}";
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", null,
                sensitiveBody.getBytes(), null);

        String described = ExternalCallLog.describe(exception);

        assertThat(described).endsWith("(status=400)");
        assertThat(described).doesNotContain("공원").doesNotContain("summary");
    }

    @Test
    @DisplayName("네트워크 오류는 원인 예외 타입까지 남겨 타임아웃을 구분할 수 있다")
    void describesNetworkFailureWithCauseType() {
        ResourceAccessException exception = new ResourceAccessException(
                "I/O error on POST request", new SocketTimeoutException("Read timed out"));

        assertThat(ExternalCallLog.describe(exception))
                .isEqualTo("ResourceAccessException(SocketTimeoutException)");
    }

    @Test
    @DisplayName("그 외 예외는 타입 이름만 남긴다")
    void describesOtherExceptionWithTypeOnly() {
        assertThat(ExternalCallLog.describe(new IOException("temp file /var/x missing")))
                .isEqualTo("IOException");
    }

    @Test
    @DisplayName("null 예외도 안전하게 처리한다")
    void describesNull() {
        assertThat(ExternalCallLog.describe(null)).isEqualTo("none");
    }
}
