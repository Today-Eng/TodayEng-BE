package com.example.todayEng.global.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

class ExternalCallLogTest {

    @Test
    void describesHttpErrorWithResponseBody() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", null,
                "reason: location is invalid"
                        .getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        assertThat(ExternalCallLog.describe(exception))
                .contains("status=400", "body=", "location is invalid");
    }

    @Test
    void limitsLongResponseBody() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", null,
                "x".repeat(2_000).getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        assertThat(ExternalCallLog.describe(exception)).hasSizeLessThan(1_100);
    }

    @Test
    void describesNetworkFailureWithCauseType() {
        ResourceAccessException exception = new ResourceAccessException(
                "I/O error on POST request", new SocketTimeoutException("Read timed out"));

        assertThat(ExternalCallLog.describe(exception))
                .isEqualTo("ResourceAccessException(SocketTimeoutException)");
    }

    @Test
    void describesOtherExceptionWithTypeOnly() {
        assertThat(ExternalCallLog.describe(new IOException("missing")))
                .isEqualTo("IOException");
    }

    @Test
    void describesNull() {
        assertThat(ExternalCallLog.describe(null)).isEqualTo("none");
    }
}
