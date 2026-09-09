package com.example.todayEng.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todayEng.domain.diary.controller.*;
import com.example.todayEng.domain.diary.service.*;
import com.example.todayEng.global.security.JwtAuthenticationFilter;
import com.example.todayEng.global.security.JwtTokenProvider;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

@WebMvcTest({DiaryController.class, DiaryAnswerController.class, DiaryCompletionController.class,
        DiaryContextController.class, DiaryDeletionController.class, DiaryMemoController.class,
        DiaryPauseController.class, DiaryQueryController.class, DiaryQuestionController.class,
        DiarySseController.class, ReflectionSessionController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
@EnableConfigurationProperties(OAuthSecurityProperties.class)
@TestPropertySource(properties = {
        "security.oauth.authorization-endpoint-permit-all=false",
        "cors.allowed-origins=http://localhost"
})
class DiaryProtectedEndpointSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean DiaryService diaryService;
    @MockBean AnswerUploadService answerUploadService;
    @MockBean DiaryAnswerQueryService diaryAnswerQueryService;
    @MockBean DiaryCompletionService diaryCompletionService;
    @MockBean DiaryContextService diaryContextService;
    @MockBean DiaryDeletionService diaryDeletionService;
    @MockBean DiaryMemoService diaryMemoService;
    @MockBean DiaryPauseService diaryPauseService;
    @MockBean DiaryQueryService diaryQueryService;
    @MockBean DiaryQuestionQueryService diaryQuestionQueryService;
    @MockBean DiarySubscriptionService diarySubscriptionService;
    @MockBean ReflectionSessionService reflectionSessionService;

    @ParameterizedTest
    @MethodSource("protectedDiaryRequests")
    void diaryEndpointWithoutAuthenticationIsUnauthorized(RequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    private static Stream<RequestBuilder> protectedDiaryRequests() {
        return Stream.of(
                post("/api/diaries"), get("/api/diaries"), get("/api/diaries/1"),
                post("/api/diaries/1/contexts"), post("/api/diaries/1/reflection-sessions"),
                get("/api/diaries/1/questions"), get("/api/diaries/1/questions/next"),
                post("/api/diaries/1/questions/2/answers"), get("/api/diaries/1/answers"),
                get("/api/diaries/1/answers/3"), get("/api/diaries/1/subscribe"),
                patch("/api/diaries/1/complete"), patch("/api/diaries/1/memo"),
                patch("/api/diaries/1/pause"), delete("/api/diaries/1")
        );
    }
}
