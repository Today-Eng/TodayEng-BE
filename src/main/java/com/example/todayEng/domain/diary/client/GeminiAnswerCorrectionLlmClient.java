package com.example.todayEng.domain.diary.client;

import com.example.todayEng.domain.diary.config.GeminiProperties;
import com.example.todayEng.domain.diary.dto.llm.AnswerCorrectionCommand;
import com.example.todayEng.domain.diary.dto.llm.AnswerCorrectionLlmResponse;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.diary.prompt.AnswerCorrectionPromptFactory;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.example.todayEng.global.log.LlmCallLog;
import com.example.todayEng.global.log.LlmFeature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class GeminiAnswerCorrectionLlmClient implements AnswerCorrectionLlmClient {

    private static final LlmFeature FEATURE = LlmFeature.ANSWER_CORRECTION;

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final AnswerCorrectionPromptFactory promptFactory;
    private final ObjectMapper objectMapper;

    public GeminiAnswerCorrectionLlmClient(@Qualifier("geminiRestClient") RestClient restClient,
            GeminiProperties properties, AnswerCorrectionPromptFactory promptFactory, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.promptFactory = promptFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public AnswerCorrectionLlmResponse correct(AnswerCorrectionCommand command) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            log.warn("LLM call failed: {}", LlmCallLog.failure(
                    FEATURE, properties.model(), "api key is not configured"));
            throw new BaseException(ErrorCode.LLM_API_FAILED);
        }
        try {
            GeminiResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("contents", List.of(Map.of("role", "user", "parts", List.of(
                                    Map.of("text", promptFactory.create(command))))),
                            "generationConfig", Map.of("temperature", 0.2,
                                    "responseMimeType", "application/json",
                                    "responseJsonSchema", schema(command.questionType()))))
                    .retrieve().body(GeminiResponse.class);
            AnswerCorrectionLlmResponse parsed = objectMapper.readValue(extract(response), AnswerCorrectionLlmResponse.class);
            validate(command.questionType(), parsed);
            return parsed;
        } catch (BaseException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw invalidResponse("response text is not valid JSON", exception);
        } catch (RestClientException exception) {
            log.warn("LLM call failed: {}",
                    LlmCallLog.failure(FEATURE, properties.model(), exception));
            throw new BaseException(ErrorCode.LLM_API_FAILED);
        }
    }

    private BaseException invalidResponse(String reason) {
        return invalidResponse(reason, null);
    }

    private BaseException invalidResponse(String reason, Throwable cause) {
        log.warn("LLM call failed: {}",
                LlmCallLog.failure(FEATURE, properties.model(), reason, cause));
        return new BaseException(ErrorCode.INVALID_LLM_RESPONSE);
    }

    private Map<String, Object> schema(QuestionType type) {
        Map<String, Object> followUp = Map.of("type", "object",
                "additionalProperties", false,
                "properties", Map.of("questionText", Map.of("type", "string"),
                        "koreanTranslation", Map.of("type", "string")),
                "required", List.of("questionText", "koreanTranslation"));
        if (type == QuestionType.FOLLOW_UP) {
            return Map.of("type", "object", "additionalProperties", false,
                    "properties", Map.of("correctedText", Map.of("type", "string"),
                            "correctionReason", Map.of("type", "string"),
                            "alternativeExpressions", Map.of("type", "array", "items", Map.of("type", "string"))),
                    "required", List.of("correctedText", "correctionReason", "alternativeExpressions"));
        }
        return Map.of("type", "object", "additionalProperties", false,
                "properties", Map.of("correctedText", Map.of("type", "string"),
                        "correctionReason", Map.of("type", "string"),
                        "alternativeExpressions", Map.of("type", "array", "items", Map.of("type", "string")),
                        "followUpQuestion", followUp),
                "required", List.of("correctedText", "correctionReason", "alternativeExpressions", "followUpQuestion"));
    }

    private String extract(GeminiResponse response) {
        if (response == null || response.candidates() == null) throw invalidResponse("response has no candidates");
        return response.candidates().stream().filter(c -> c != null && c.content() != null)
                .flatMap(c -> c.content().parts().stream()).filter(p -> p != null).map(Part::text)
                .filter(t -> t != null && !t.isBlank()).findFirst()
                .orElseThrow(() -> invalidResponse("response has no usable text"));
    }

    private void validate(QuestionType type, AnswerCorrectionLlmResponse r) {
        if (r == null || blank(r.correctedText()) || blank(r.correctionReason()) || r.alternativeExpressions() == null
                || (type == QuestionType.MAIN && (r.followUpQuestion() == null
                || blank(r.followUpQuestion().questionText()) || blank(r.followUpQuestion().koreanTranslation())))
                || (type == QuestionType.FOLLOW_UP && r.followUpQuestion() != null)) {
            throw invalidResponse("response does not satisfy the correction schema");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private record GeminiResponse(List<Candidate> candidates) { }
    private record Candidate(Content content) { }
    private record Content(List<Part> parts) { private Content { parts = parts == null ? List.of() : parts; } }
    private record Part(String text) { }
}
