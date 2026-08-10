package com.example.todayEng.global.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

class LlmCallLogTest {

    private static final String QUOTA_EXCEEDED_BODY = """
            {
              "error": {
                "code": 429,
                "message": "You exceeded your current quota, please check your plan and billing details.",
                "status": "RESOURCE_EXHAUSTED",
                "details": [
                  {
                    "@type": "type.googleapis.com/google.rpc.QuotaFailure",
                    "violations": [
                      {
                        "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
                        "quotaId": "GenerateRequestsPerDayPerProjectPerModel-FreeTier",
                        "quotaValue": "50"
                      }
                    ]
                  },
                  {
                    "@type": "type.googleapis.com/google.rpc.RetryInfo",
                    "retryDelay": "37s"
                  }
                ]
              }
            }
            """;

    @Test
    @DisplayName("429 응답은 상태 코드, 쿼터 위반, 재시도 지연까지 남긴다")
    void describesQuotaExceeded() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null,
                QUOTA_EXCEEDED_BODY.getBytes(), null);

        String described = LlmCallLog.failure(
                LlmFeature.DIARY_IMAGE_ANALYSIS, "gemini-2.5-flash", exception);

        assertThat(described)
                .contains("feature=diary-image-analysis")
                .contains("model=gemini-2.5-flash")
                .contains("status=429")
                .contains("apiStatus=RESOURCE_EXHAUSTED")
                .contains("GenerateRequestsPerDayPerProjectPerModel-FreeTier(limit=50)")
                .contains("retryDelay=37s")
                .contains("message=You exceeded your current quota");
    }

    @Test
    @DisplayName("본문에 RetryInfo가 없으면 Retry-After 헤더로 대체한다")
    void fallsBackToRetryAfterHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "60");
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers,
                "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\"}}".getBytes(), null);

        assertThat(LlmCallLog.failure(
                LlmFeature.REFLECTION_QUESTION, "gemini-2.5-flash", exception))
                .contains("retryDelay=60");
    }

    @Test
    @DisplayName("Google 오류 규격이 아닌 응답 본문은 상태 코드만 남기고 내용은 남기지 않는다")
    void doesNotLeakNonStandardResponseBody() {
        String sensitiveBody = "{\"summary\":\"공원에서 친구와 점심\"}";
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", null,
                sensitiveBody.getBytes(), null);

        String described = LlmCallLog.failure(
                LlmFeature.ANSWER_CORRECTION, "gemini-2.5-flash", exception);

        assertThat(described).contains("status=400");
        assertThat(described).doesNotContain("공원").doesNotContain("summary");
    }

    @Test
    @DisplayName("오류 메시지는 한 줄로 접고 200자를 넘으면 잘라낸다")
    void truncatesLongMessage() {
        String longMessage = "a".repeat(300);
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", null,
                ("{\"error\":{\"message\":\"" + longMessage + "\"}}").getBytes(), null);

        String described = LlmCallLog.failure(
                LlmFeature.ANSWER_CORRECTION, "gemini-2.5-flash", exception);

        assertThat(described).contains("...(truncated)");
        assertThat(described).doesNotContain("a".repeat(201));
    }

    @Test
    @DisplayName("네트워크 오류는 원인 예외 타입까지 남겨 타임아웃을 구분할 수 있다")
    void describesNetworkFailureWithCauseType() {
        ResourceAccessException exception = new ResourceAccessException(
                "I/O error on POST request", new SocketTimeoutException("Read timed out"));

        assertThat(LlmCallLog.failure(
                LlmFeature.DIARY_MEMORY_ANALYSIS, "gemini-2.5-flash", exception))
                .isEqualTo("feature=diary-memory-analysis, model=gemini-2.5-flash, "
                        + "type=ResourceAccessException, cause=SocketTimeoutException");
    }

    @Test
    @DisplayName("예외 없는 실패는 사유만 남긴다")
    void describesReasonOnlyFailure() {
        assertThat(LlmCallLog.failure(
                LlmFeature.REFLECTION_QUESTION, "gemini-2.5-flash", "api key is not configured"))
                .isEqualTo("feature=reflection-question, model=gemini-2.5-flash, "
                        + "reason=api key is not configured");
    }

    @Test
    @DisplayName("모델명이 비어 있어도 로그 생성에 실패하지 않는다")
    void handlesMissingModel() {
        assertThat(LlmCallLog.failure(LlmFeature.REFLECTION_QUESTION, "  ", "boom"))
                .contains("model=unknown");
    }
}
