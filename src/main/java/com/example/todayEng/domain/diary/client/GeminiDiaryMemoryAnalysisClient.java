package com.example.todayEng.domain.diary.client;

import com.example.todayEng.domain.diary.config.GeminiProperties;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse;
import com.example.todayEng.domain.diary.prompt.DiaryMemoryPromptFactory;
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
public class GeminiDiaryMemoryAnalysisClient
        implements DiaryMemoryAnalysisClient {

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final DiaryMemoryPromptFactory promptFactory;
    private final ObjectMapper objectMapper;

    public GeminiDiaryMemoryAnalysisClient(
            @Qualifier("geminiRestClient") RestClient restClient,
            GeminiProperties properties,
            DiaryMemoryPromptFactory promptFactory,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.promptFactory = promptFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public DiaryMemoryAnalysisResponse analyze(
            DiaryMemoryAnalysisCommand command
    ) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new BaseException(ErrorCode.LLM_API_FAILED);
        }

        try {
            GeminiResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent",
                            properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createRequest(promptFactory.create(command)))
                    .retrieve()
                    .body(GeminiResponse.class);
            return objectMapper.readValue(
                    extractText(response),
                    DiaryMemoryAnalysisResponse.class
            );
        } catch (BaseException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw new BaseException(ErrorCode.INVALID_LLM_RESPONSE);
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
                        "temperature", 0.2,
                        "responseMimeType", "application/json",
                        "responseJsonSchema", responseSchema()
                )
        );
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> item = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "value", Map.of("type", "string"),
                        "sourceDiaryIds", Map.of(
                                "type", "array",
                                "items", Map.of("type", "integer")
                        )
                ),
                "required", List.of("value", "sourceDiaryIds")
        );
        Map<String, Object> items = Map.of(
                "type", "array",
                "maxItems", 5,
                "items", item
        );

        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "people", items,
                        "places", items,
                        "themes", items,
                        "ongoingStories", items,
                        "recentEmotions", items
                ),
                "required", List.of(
                        "people",
                        "places",
                        "themes",
                        "ongoingStories",
                        "recentEmotions"
                )
        );
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null) {
            throw new BaseException(ErrorCode.INVALID_LLM_RESPONSE);
        }
        return response.candidates().stream()
                .filter(candidate -> candidate.content() != null)
                .flatMap(candidate -> candidate.content().parts().stream())
                .map(Part::text)
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElseThrow(() ->
                        new BaseException(ErrorCode.INVALID_LLM_RESPONSE));
    }

    private record GeminiResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }

    private record Content(List<Part> parts) {
        private Content {
            parts = parts == null ? List.of() : parts;
        }
    }

    private record Part(String text) {
    }
}
