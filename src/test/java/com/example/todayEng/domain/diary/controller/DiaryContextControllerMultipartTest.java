package com.example.todayEng.domain.diary.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryContextCreateResponse;
import com.example.todayEng.domain.diary.service.DiaryContextService;
import com.example.todayEng.global.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

@WebMvcTest(controllers = DiaryContextController.class)
@AutoConfigureMockMvc(addFilters = false)
class DiaryContextControllerMultipartTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DiaryContextService diaryContextService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        given(diaryContextService.createContexts(any(), any(), any(), any()))
                .willReturn(new DiaryContextCreateResponse(1L, List.of()));
    }

    @Test
    @DisplayName("request part에 Content-Type이 없어도 JSON으로 바인딩된다")
    void bindsRequestPartWithoutContentType() throws Exception {
        MockPart requestPart = new MockPart(
                "request",
                "{\"memo\":\"점심\",\"location\":{\"latitude\":37.5,\"longitude\":127.0}}"
                        .getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/diaries/1/contexts")
                        .part(requestPart)
                        .file(new MockMultipartFile(
                                "images", "day.jpg", "image/jpeg", new byte[]{1, 2, 3})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        DiaryContextCreateRequest captured = capturedRequest();
        assertThat(captured.memo()).isEqualTo("점심");
        assertThat(captured.location().latitude()).isEqualTo(37.5);
        assertThat(captured.location().longitude()).isEqualTo(127.0);
    }

    @Test
    @DisplayName("request part에 application/json을 붙여도 그대로 바인딩된다")
    void bindsRequestPartWithJsonContentType() throws Exception {
        MockPart requestPart = new MockPart(
                "request", "{\"memo\":\"저녁\"}".getBytes(StandardCharsets.UTF_8));
        requestPart.getHeaders().setContentType(
                org.springframework.http.MediaType.APPLICATION_JSON);

        mockMvc.perform(multipart("/api/diaries/1/contexts").part(requestPart))
                .andExpect(status().isOk());

        assertThat(capturedRequest().memo()).isEqualTo("저녁");
    }

    @Test
    @DisplayName("이미지만 보내고 request part를 생략해도 처리된다")
    void bindsImagesOnlyWithoutRequestPart() throws Exception {
        mockMvc.perform(multipart("/api/diaries/1/contexts")
                        .file(new MockMultipartFile(
                                "images", "day.jpg", "image/jpeg", new byte[]{1, 2, 3})))
                .andExpect(status().isOk());

        assertThat(capturedRequest())
                .isEqualTo(new DiaryContextCreateRequest(null, null));
    }

    @Test
    @DisplayName("request part가 JSON null이면 빈 요청으로 정규화된다")
    void normalizesJsonNullPart() throws Exception {
        MockPart requestPart = new MockPart(
                "request", "null".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/diaries/1/contexts").part(requestPart))
                .andExpect(status().isOk());

        assertThat(capturedRequest())
                .isEqualTo(new DiaryContextCreateRequest(null, null));
    }

    @Test
    @DisplayName("깨진 JSON은 C008로 거부된다")
    void rejectsMalformedRequestPart() throws Exception {
        MockPart requestPart = new MockPart(
                "request", "{\"memo\":".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/diaries/1/contexts").part(requestPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C008"));
    }

    private DiaryContextCreateRequest capturedRequest() {
        ArgumentCaptor<DiaryContextCreateRequest> captor =
                ArgumentCaptor.forClass(DiaryContextCreateRequest.class);
        verify(diaryContextService).createContexts(any(), any(), captor.capture(), any());
        return captor.getValue();
    }
}
