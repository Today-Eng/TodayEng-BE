package com.example.todayEng.domain.diary.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.todayEng.domain.diary.config.GeminiProperties;
import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand;
import com.example.todayEng.domain.diary.prompt.ReflectionQuestionPromptFactory;
import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiReflectionQuestionLlmClientTest {

    private MockRestServiceServer server;
    private GeminiReflectionQuestionLlmClient client;
    private ReflectionQuestionPromptFactory promptFactory;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com");
        server = MockRestServiceServer.bindTo(builder).build();
        promptFactory = mock(ReflectionQuestionPromptFactory.class);
        client = new GeminiReflectionQuestionLlmClient(
                builder.build(),
                new GeminiProperties(
                        URI.create("https://generativelanguage.googleapis.com"),
                        "test-key",
                        "gemini-2.5-flash",
                        3,
                        30
                ),
                promptFactory,
                new ObjectMapper()
        );
    }

    @Test
    void parsesStructuredJsonResponse() {
        var command = command();
        given(promptFactory.create(command)).willReturn("prompt");
        String response = """
                {"candidates":[{"content":{"parts":[{"text":"{\\"questions\\":[{\\"order\\":1,\\"questionText\\":\\"Q1\\",\\"koreanTranslation\\":\\"질문1\\",\\"keyword\\":\\"memo\\",\\"contextId\\":100},{\\"order\\":2,\\"questionText\\":\\"Q2\\",\\"koreanTranslation\\":\\"질문2\\",\\"keyword\\":\\"memo\\",\\"contextId\\":100},{\\"order\\":3,\\"questionText\\":\\"Q3\\",\\"koreanTranslation\\":\\"질문3\\",\\"keyword\\":\\"memo\\",\\"contextId\\":100}]}"}]}}]}
                """;
        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
                ))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        var result = client.generateQuestions(command);

        assertThat(result.questions()).hasSize(3);
        assertThat(result.questions().get(0).contextId()).isEqualTo(100L);
        server.verify();
    }

    @Test
    void convertsMalformedJsonToDomainError() {
        var command = command();
        given(promptFactory.create(command)).willReturn("prompt");
        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
                ))
                .andRespond(withSuccess(
                        "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"not-json\"}]}}]}",
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> client.generateQuestions(command))
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.INVALID_LLM_RESPONSE);
    }

    @Test
    void convertsNullCandidateElementsToDomainError() {
        var command = command();
        given(promptFactory.create(command)).willReturn("prompt");
        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
                ))
                .andRespond(withSuccess(
                        "{\"candidates\":[null,{\"content\":{\"parts\":[null]}}]}",
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> client.generateQuestions(command))
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.INVALID_LLM_RESPONSE);
    }

    private ReflectionQuestionGenerationCommand command() {
        return new ReflectionQuestionGenerationCommand(
                1L,
                10L,
                1L,
                "성연",
                EnglishLevel.BEGINNER,
                List.of(),
                List.of()
        );
    }
}
