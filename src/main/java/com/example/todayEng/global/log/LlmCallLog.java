package com.example.todayEng.global.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * LLM 호출 실패를 모든 클라이언트가 동일한 형식으로 기록한다.
 *
 * <p>응답 본문을 그대로 남기지 않고 Google API 오류 규격에서 진단에 필요한 필드만 골라
 * 기록한다. API 키, 인증 헤더, 요청 본문은 어떤 경우에도 포함하지 않는다.
 */
public final class LlmCallLog {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_MESSAGE_LENGTH = 200;
    private static final String QUOTA_FAILURE_TYPE =
            "type.googleapis.com/google.rpc.QuotaFailure";
    private static final String RETRY_INFO_TYPE =
            "type.googleapis.com/google.rpc.RetryInfo";

    private LlmCallLog() {
    }

    public static String failure(LlmFeature feature, String model, Throwable throwable) {
        return failure(feature, model, null, throwable);
    }

    public static String failure(LlmFeature feature, String model, String reason) {
        return failure(feature, model, reason, null);
    }

    public static String failure(
            LlmFeature feature,
            String model,
            String reason,
            Throwable throwable
    ) {
        StringJoiner joiner = head(feature, model);
        if (reason != null) {
            joiner.add("reason=" + sanitize(reason));
        }
        if (throwable != null) {
            appendThrowable(joiner, throwable);
        }
        return joiner.toString();
    }

    private static void appendThrowable(StringJoiner joiner, Throwable throwable) {
        joiner.add("type=" + throwable.getClass().getSimpleName());

        if (throwable instanceof RestClientResponseException responseException) {
            appendHttpDetails(joiner, responseException);
        } else if (throwable instanceof ResourceAccessException
                && throwable.getCause() != null) {
            joiner.add("cause=" + throwable.getCause().getClass().getSimpleName());
        }
    }

    private static StringJoiner head(LlmFeature feature, String model) {
        StringJoiner joiner = new StringJoiner(", ");
        joiner.add("feature=" + (feature == null ? "unknown" : feature.value()));
        joiner.add("model=" + (model == null || model.isBlank() ? "unknown" : model));
        return joiner;
    }

    private static void appendHttpDetails(
            StringJoiner joiner,
            RestClientResponseException exception
    ) {
        joiner.add("status=" + exception.getStatusCode().value());

        JsonNode error = readError(exception.getResponseBodyAsString());
        String apiStatus = text(error, "status");
        if (apiStatus != null) {
            joiner.add("apiStatus=" + apiStatus);
        }

        List<String> quotas = quotaViolations(error);
        if (!quotas.isEmpty()) {
            joiner.add("quota=" + quotas);
        }

        String retryDelay = retryDelay(error, exception);
        if (retryDelay != null) {
            joiner.add("retryDelay=" + retryDelay);
        }

        String message = text(error, "message");
        if (message != null) {
            joiner.add("message=" + sanitize(message));
        }
    }

    private static JsonNode readError(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(body).path("error");
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private static String text(JsonNode error, String field) {
        if (error == null) {
            return null;
        }
        String value = error.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private static List<String> quotaViolations(JsonNode error) {
        List<String> violations = new ArrayList<>();
        if (error == null) {
            return violations;
        }
        for (JsonNode detail : error.path("details")) {
            if (!QUOTA_FAILURE_TYPE.equals(detail.path("@type").asText())) {
                continue;
            }
            for (JsonNode violation : detail.path("violations")) {
                String name = violation.path("quotaId").asText(null);
                if (name == null || name.isBlank()) {
                    name = violation.path("quotaMetric").asText(null);
                }
                if (name == null || name.isBlank()) {
                    continue;
                }
                String limit = violation.path("quotaValue").asText(null);
                violations.add(limit == null || limit.isBlank()
                        ? name
                        : name + "(limit=" + limit + ")");
            }
        }
        return violations;
    }

    private static String retryDelay(
            JsonNode error,
            RestClientResponseException exception
    ) {
        if (error != null) {
            for (JsonNode detail : error.path("details")) {
                if (!RETRY_INFO_TYPE.equals(detail.path("@type").asText())) {
                    continue;
                }
                String delay = detail.path("retryDelay").asText(null);
                if (delay != null && !delay.isBlank()) {
                    return delay;
                }
            }
        }
        HttpHeaders headers = exception.getResponseHeaders();
        if (headers == null) {
            return null;
        }
        String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
        return retryAfter == null || retryAfter.isBlank() ? null : retryAfter;
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "none";
        }
        String singleLine = value.replaceAll("\\s+", " ").trim();
        if (singleLine.isEmpty()) {
            return "none";
        }
        return singleLine.length() <= MAX_MESSAGE_LENGTH
                ? singleLine
                : singleLine.substring(0, MAX_MESSAGE_LENGTH) + "...(truncated)";
    }
}
