package com.example.todayEng.domain.diary.client;

import com.example.todayEng.domain.diary.config.GeminiProperties;
import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand;
import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionLlmResponse;
import com.example.todayEng.domain.diary.prompt.ReflectionQuestionPromptFactory;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GeminiReflectionQuestionLlmClient
        implements ReflectionQuestionLlmClient {

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final ReflectionQuestionPromptFactory promptFactory;
    private final ObjectMapper objectMapper;

    public GeminiReflectionQuestionLlmClient(
            @Qualifier("geminiRestClient") RestClient restClient,
            GeminiProperties properties,
            ReflectionQuestionPromptFactory promptFactory,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.promptFactory = promptFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReflectionQuestionLlmResponse generateQuestions(
            ReflectionQuestionGenerationCommand command
    ) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new BaseException(ErrorCode.LLM_API_FAILED);
        }

        try {
            GeminiGenerateResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createRequest(promptFactory.create(command)))
                    .retrieve()
                    .body(GeminiGenerateResponse.class);

            return parseResponse(response);
        } catch (BaseException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BaseException(ErrorCode.LLM_API_FAILED);
        }
    }

    private Map<String, Object> createRequest(String prompt) {
        return Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "responseMimeType", "application/json",
                        "responseJsonSchema", responseSchema()
                )
        );
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> question = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "order", Map.of("type", "integer", "minimum", 1, "maximum", 3),
                        "questionText", Map.of("type", "string"),
                        "koreanTranslation", Map.of("type", "string"),
                        "keyword", Map.of("type", "string"),
                        "contextId", Map.of(
                                "type", "integer",
                                "nullable", true
                        )
                ),
                "required", List.of(
                        "order",
                        "questionText",
                        "koreanTranslation",
                        "keyword",
                        "contextId"
                )
        );

        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "questions", Map.of(
                                "type", "array",
                                "minItems", 3,
                                "maxItems", 3,
                                "items", question
                        )
                ),
                "required", List.of("questions")
        );
    }

    private ReflectionQuestionLlmResponse parseResponse(
            GeminiGenerateResponse response
    ) {
        String text = extractText(response);
        try {
            return objectMapper.readValue(
                    text,
                    ReflectionQuestionLlmResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new BaseException(ErrorCode.INVALID_LLM_RESPONSE);
        }
    }

    private String extractText(GeminiGenerateResponse response) {
        if (response == null || response.candidates() == null) {
            throw new BaseException(ErrorCode.INVALID_LLM_RESPONSE);
        }

        return response.candidates().stream()
                .filter(candidate -> candidate.content() != null)
                .flatMap(candidate -> candidate.content().parts().stream())
                .map(GeminiPart::text)
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElseThrow(() ->
                        new BaseException(ErrorCode.INVALID_LLM_RESPONSE)
                );
    }

    private record GeminiGenerateResponse(List<GeminiCandidate> candidates) {
    }

    private record GeminiCandidate(GeminiContent content) {
    }

    private record GeminiContent(List<GeminiPart> parts) {
        private GeminiContent {
            parts = parts == null ? List.of() : parts;
        }
    }

    private record GeminiPart(String text) {
    }
}
