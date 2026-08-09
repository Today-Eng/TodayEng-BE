package com.example.todayEng.domain.diary.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand.DiaryInput;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse;
import com.example.todayEng.domain.diary.prompt.DiaryMemoryPromptFactory;
import com.example.todayEng.domain.diary.service.DiaryMemoryResultValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Manual live benchmark for issue #100; skipped during normal test runs. */
class DiaryMemoryCandidateBenchmarkTest {

    private static final List<Integer> COUNTS = List.of(3, 5, 10, 20);
    private static final String FIXTURE = "/benchmark/diary-memory-candidates.json";
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final DiaryMemoryPromptFactory prompts = new DiaryMemoryPromptFactory(mapper);
    private final DiaryMemoryResultValidator validator = new DiaryMemoryResultValidator();

    @Test
    void fixtureKeepsAllConditionsIdenticalAndPeopleRepeat() throws Exception {
        List<DiaryInput> diaries = fixture();

        assertThat(diaries).hasSize(20);
        assertThat(diaries).isSortedAccordingTo((left, right) ->
                right.diaryDate().compareTo(left.diaryDate()));
        for (String person : List.of("가윤", "현경", "성연", "희원", "은우")) {
            long evidenceCount = diaries.stream()
                    .filter(diary -> contains(diary, person))
                    .count();
            assertThat(evidenceCount).as(person + " evidence diaries")
                    .isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void compareCandidateCounts() throws Exception {
        Assumptions.assumeTrue(Boolean.parseBoolean(
                        System.getenv("DIARY_MEMORY_BENCHMARK")),
                "Enable with DIARY_MEMORY_BENCHMARK=true");
        String apiKey = System.getenv("GEMINI_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "GEMINI_API_KEY is required");
        int repetitions = Integer.parseInt(env(
                "DIARY_MEMORY_BENCHMARK_RUNS", "3"));
        String model = env("GEMINI_MODEL", "gemini-2.5-flash");
        RestClient client = client(URI.create(env("GEMINI_BASE_URL",
                "https://generativelanguage.googleapis.com")));
        List<DiaryInput> fixture = fixture();
        assertThat(fixture).hasSizeGreaterThanOrEqualTo(20);

        List<Metric> metrics = new ArrayList<>();
        for (int count : COUNTS) {
            var command = new DiaryMemoryAnalysisCommand(999L,
                    List.copyOf(fixture.subList(0, count)));
            for (int run = 1; run <= repetitions; run++) {
                metrics.add(measure(client, apiKey, model, count, run, command));
            }
        }

        Path output = Path.of(System.getProperty("diaryMemoryBenchmarkOutput",
                "build/reports/diary-memory-benchmark"));
        Files.createDirectories(output);
        Files.writeString(output.resolve("runs.csv"), csv(metrics), StandardCharsets.UTF_8);
        Files.writeString(output.resolve("summary.md"),
                markdown(model, repetitions, metrics), StandardCharsets.UTF_8);
        assertThat(metrics).allSatisfy(metric -> assertThat(metric.error()).isBlank());
    }

    private Metric measure(RestClient client, String key, String model, int count,
                           int run, DiaryMemoryAnalysisCommand command) {
        long totalStart = System.nanoTime();
        String prompt = prompts.create(command);
        long llmStart = System.nanoTime();
        try {
            JsonNode raw = client.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request(prompt)).retrieve().body(JsonNode.class);
            long llmMs = millis(llmStart);
            DiaryMemoryAnalysisResponse response = mapper.readValue(
                    text(raw), DiaryMemoryAnalysisResponse.class);
            validator.validate(command, response);
            int people = response.people().size();
            int places = response.places().size();
            int themes = response.themes().size();
            int stories = response.ongoingStories().size();
            int emotions = response.recentEmotions().size();
            JsonNode usage = raw == null ? null : raw.path("usageMetadata");
            return new Metric(count, run, prompt.length(), bytes(prompt),
                    integer(usage, "promptTokenCount"),
                    integer(usage, "candidatesTokenCount"), llmMs,
                    millis(totalStart), people + places + themes + stories + emotions,
                    people, places, themes, stories, emotions,
                    mapper.writeValueAsString(response), "");
        } catch (Exception exception) {
            return new Metric(count, run, prompt.length(), bytes(prompt), null, null,
                    millis(llmStart), millis(totalStart), 0, 0, 0, 0, 0, 0, "",
                    exception.getClass().getSimpleName() + ": "
                            + Optional.ofNullable(exception.getMessage()).orElse(""));
        }
    }

    private List<DiaryInput> fixture() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(FIXTURE)) {
            if (input == null) throw new IllegalStateException("Missing " + FIXTURE);
            return mapper.readValue(input, new TypeReference<>() {});
        }
    }

    private boolean contains(DiaryInput diary, String value) {
        if (diary.memo() != null && diary.memo().contains(value)) return true;
        return diary.reflections().stream().anyMatch(reflection ->
                reflection.question().contains(value)
                        || reflection.answer().contains(value));
    }

    private RestClient client(URI baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(90));
        return RestClient.builder().baseUrl(baseUrl.toString())
                .requestFactory(factory).build();
    }

    private Map<String, Object> request(String prompt) {
        return Map.of("contents", List.of(Map.of("role", "user", "parts",
                        List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.2,
                        "responseMimeType", "application/json",
                        "responseJsonSchema", schema()));
    }

    private Map<String, Object> schema() {
        Map<String, Object> item = Map.of("type", "object",
                "additionalProperties", false, "properties", Map.of(
                        "value", Map.of("type", "string"),
                        "sourceDiaryIds", Map.of("type", "array", "items",
                                Map.of("type", "integer"))),
                "required", List.of("value", "sourceDiaryIds"));
        Map<String, Object> items = Map.of("type", "array", "maxItems", 5,
                "items", item);
        return Map.of("type", "object", "additionalProperties", false,
                "properties", Map.of("people", items, "places", items,
                        "themes", items, "ongoingStories", items,
                        "recentEmotions", items),
                "required", List.of("people", "places", "themes",
                        "ongoingStories", "recentEmotions"));
    }

    private String text(JsonNode response) {
        JsonNode text = response == null ? null
                : response.at("/candidates/0/content/parts/0/text");
        if (text == null || !text.isTextual() || text.asText().isBlank()) {
            throw new IllegalStateException("Gemini response has no text");
        }
        return text.asText();
    }

    private Integer integer(JsonNode node, String field) {
        return node != null && node.path(field).canConvertToInt()
                ? node.path(field).intValue() : null;
    }

    private String csv(List<Metric> metrics) {
        StringBuilder out = new StringBuilder("candidateCount,run,promptChars,promptBytes,inputTokens,outputTokens,llmMs,totalMs,validMemories,people,places,themes,ongoingStories,recentEmotions,extracted,error\n");
        for (Metric m : metrics) {
            out.append(m.candidateCount()).append(',').append(m.run()).append(',')
                    .append(m.promptChars()).append(',').append(m.promptBytes()).append(',')
                    .append(value(m.inputTokens())).append(',').append(value(m.outputTokens())).append(',')
                    .append(m.llmMs()).append(',').append(m.totalMs()).append(',')
                    .append(m.validMemories()).append(',').append(m.people()).append(',')
                    .append(m.places()).append(',').append(m.themes()).append(',')
                    .append(m.stories()).append(',').append(m.emotions()).append(',')
                    .append(quote(m.extracted())).append(',').append(quote(m.error())).append('\n');
        }
        return out.toString();
    }

    private String markdown(String model, int repetitions, List<Metric> metrics) {
        StringBuilder out = new StringBuilder("# 과거 회고 후보 수 벤치마크 결과\n\n")
                .append("- model: ").append(model).append("\n- repetitions: ")
                .append(repetitions).append("\n- window: 최근 30일\n\n")
                .append("| 후보 수 | 성공/전체 | 평균 input tokens | 평균 LLM ms | 평균 total ms | 평균 유효 기억 수 |\n")
                .append("|---:|---:|---:|---:|---:|---:|\n");
        for (int count : COUNTS) {
            List<Metric> group = metrics.stream().filter(m -> m.candidateCount() == count).toList();
            List<Metric> ok = group.stream().filter(m -> m.error().isBlank()).toList();
            out.append('|').append(count).append('|').append(ok.size()).append('/')
                    .append(group.size()).append('|').append(avgInt(ok.stream().map(Metric::inputTokens).toList()))
                    .append('|').append(avgLong(ok.stream().map(Metric::llmMs).toList()))
                    .append('|').append(avgLong(ok.stream().map(Metric::totalMs).toList()))
                    .append('|').append(avgInt(ok.stream().map(Metric::validMemories).toList())).append("|\n");
        }
        return out.append("\n상세 추출 결과와 오류는 runs.csv를 확인합니다.\n").toString();
    }

    private String avgInt(List<Integer> values) {
        var present = values.stream().filter(Objects::nonNull).toList();
        return present.isEmpty() ? "-" : format(present.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private String avgLong(List<Long> values) {
        return values.isEmpty() ? "-" : format(values.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    private String format(double value) { return String.format(Locale.ROOT, "%.1f", value); }
    private String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
    private long millis(long start) { return Duration.ofNanos(System.nanoTime() - start).toMillis(); }
    private int bytes(String value) { return value.getBytes(StandardCharsets.UTF_8).length; }
    private String value(Object value) { return value == null ? "" : value.toString(); }
    private String quote(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }

    private record Metric(int candidateCount, int run, int promptChars,
                          int promptBytes, Integer inputTokens, Integer outputTokens,
                          long llmMs, long totalMs, int validMemories, int people,
                          int places, int themes, int stories, int emotions,
                          String extracted, String error) {}
}
