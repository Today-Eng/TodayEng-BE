package com.example.todayEng.domain.diary.client;

import com.example.todayEng.domain.diary.config.GeminiProperties;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.example.todayEng.global.log.LlmCallLog;
import com.example.todayEng.global.log.LlmFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class GeminiDiaryImageAnalysisClient implements DiaryImageAnalysisClient {

    private static final LlmFeature FEATURE = LlmFeature.DIARY_IMAGE_ANALYSIS;
    private static final String API_URI =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";
    private static final String PROMPT = """
            Analyze these diary photos as context for generating reflective questions.
            Describe only visible, non-sensitive facts. Do not identify people or infer sensitive traits.
            Summarize the scene, activities, notable objects, mood cues, and connections across photos.
            Write all string values in Korean.
            """;

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiDiaryImageAnalysisClient(
            @Qualifier("geminiRestClient") RestClient restClient,
            GeminiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode analyze(List<MultipartFile> images) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            log.warn("LLM call failed: {}", LlmCallLog.failure(
                    FEATURE, properties.model(), "api key is not configured"));
            throw new BaseException(ErrorCode.EXTERNAL_API_ERROR);
        }

        try {
            JsonNode response = restClient.post()
                    .uri(API_URI, properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createRequest(images))
                    .retrieve()
                    .body(JsonNode.class);
            String analysis = response == null ? null : response
                    .path("candidates").path(0).path("content")
                    .path("parts").path(0).path("text").asText(null);
            if (analysis == null || analysis.isBlank()) {
                int candidateCount = response == null
                        ? 0
                        : response.path("candidates").size();
                log.warn("LLM call failed: {}, candidateCount={}",
                        LlmCallLog.failure(FEATURE, properties.model(),
                                "response has no usable text"),
                        candidateCount);
                throw new BaseException(ErrorCode.EXTERNAL_API_ERROR);
            }
            return objectMapper.readTree(analysis);
        } catch (BaseException exception) {
            throw exception;
        } catch (IOException | RestClientException exception) {
            log.warn("LLM call failed: {}, imageCount={}",
                    LlmCallLog.failure(FEATURE, properties.model(), exception),
                    images.size());
            throw new BaseException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    private Map<String, Object> createRequest(List<MultipartFile> images)
            throws IOException {
        List<Map<String, Object>> parts = new ArrayList<>();
        for (MultipartFile image : images) {
            parts.add(Map.of("inline_data", Map.of(
                    "mime_type", image.getContentType(),
                    "data", Base64.getEncoder().encodeToString(image.getBytes())
            )));
        }
        parts.add(Map.of("text", PROMPT));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "summary", Map.of("type", "string"),
                "scenes", Map.of("type", "array", "items", Map.of("type", "string")),
                "activities", Map.of("type", "array", "items", Map.of("type", "string")),
                "objects", Map.of("type", "array", "items", Map.of("type", "string")),
                "moodCues", Map.of("type", "array", "items", Map.of("type", "string"))
        ));
        schema.put("required", List.of(
                "summary", "scenes", "activities", "objects", "moodCues"));

        return Map.of(
                "contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", schema
                )
        );
    }
}
