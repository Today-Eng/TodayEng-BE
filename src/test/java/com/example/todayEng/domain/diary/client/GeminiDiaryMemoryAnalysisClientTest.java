package com.example.todayEng.domain.diary.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.todayEng.domain.diary.config.GeminiProperties;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand.DiaryInput;
import com.example.todayEng.domain.diary.prompt.DiaryMemoryPromptFactory;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiDiaryMemoryAnalysisClientTest {

    private MockRestServiceServer server;
    private GeminiDiaryMemoryAnalysisClient client;
    private DiaryMemoryPromptFactory promptFactory;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com");
        server = MockRestServiceServer.bindTo(builder).build();
        promptFactory = mock(DiaryMemoryPromptFactory.class);
        objectMapper = new ObjectMapper();
        client = new GeminiDiaryMemoryAnalysisClient(
                builder.build(),
                new GeminiProperties(
                        URI.create("https://generativelanguage.googleapis.com"),
                        "test-key",
                        "gemini-2.5-flash",
                        3,
                        30
                ),
                promptFactory,
                objectMapper
        );
    }

    @Test
    void parsesStructuredMemoryResponse() throws Exception {
        var command = command();
        given(promptFactory.create(command)).willReturn("prompt");
        String memory = objectMapper.writeValueAsString(Map.of(
                "people", List.of(),
                "places", List.of(Map.of(
                        "value", "Seongsu",
                        "sourceDiaryIds", List.of(1L, 2L)
                )),
                "themes", List.of(),
                "ongoingStories", List.of(),
                "recentEmotions", List.of()
        ));
        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
                ))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andRespond(withSuccess(
                        geminiResponse(memory),
                        MediaType.APPLICATION_JSON
                ));

        var result = client.analyze(command);

        assertThat(result.places()).singleElement().satisfies(item -> {
            assertThat(item.value()).isEqualTo("Seongsu");
            assertThat(item.sourceDiaryIds()).containsExactly(1L, 2L);
        });
        server.verify();
    }

    @Test
    void convertsMalformedJsonToDomainError() throws Exception {
        var command = command();
        given(promptFactory.create(command)).willReturn("prompt");
        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
                ))
                .andRespond(withSuccess(
                        geminiResponse("not-json"),
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> client.analyze(command))
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_LLM_RESPONSE));
    }

    private String geminiResponse(String text) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of(
                                "parts", List.of(Map.of("text", text))
                        )
                ))
        ));
    }

    private DiaryMemoryAnalysisCommand command() {
        return new DiaryMemoryAnalysisCommand(
                10L,
                List.of(new DiaryInput(
                        1L,
                        LocalDate.of(2026, 7, 1),
                        "memo",
                        List.of()
                ))
        );
    }
}
