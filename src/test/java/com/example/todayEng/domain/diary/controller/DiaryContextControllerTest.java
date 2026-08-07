package com.example.todayEng.domain.diary.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryContextCreateResponse;
import com.example.todayEng.domain.diary.service.DiaryContextService;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiaryContextControllerTest {

    @Mock
    private DiaryContextService diaryContextService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DiaryContextController controller;

    @Test
    void normalizesJsonNullRequestPartToEmptyRequest() {
        given(diaryContextService.createContexts(eq(1L), eq(10L), any(), any()))
                .willReturn(new DiaryContextCreateResponse(10L, List.of()));

        controller.createContexts(1L, 10L, "null", null);

        assertThat(capturedRequest()).isEqualTo(
                new DiaryContextCreateRequest(null, null));
    }

    @Test
    void normalizesMissingRequestPartToEmptyRequest() {
        given(diaryContextService.createContexts(eq(1L), eq(10L), any(), any()))
                .willReturn(new DiaryContextCreateResponse(10L, List.of()));

        controller.createContexts(1L, 10L, null, null);

        assertThat(capturedRequest()).isEqualTo(
                new DiaryContextCreateRequest(null, null));
    }

    @Test
    void parsesRequestPartWithoutContentType() {
        given(diaryContextService.createContexts(eq(1L), eq(10L), any(), any()))
                .willReturn(new DiaryContextCreateResponse(10L, List.of()));

        controller.createContexts(1L, 10L,
                "{\"memo\":\"점심\",\"location\":{\"latitude\":37.5,\"longitude\":127.0}}", null);

        DiaryContextCreateRequest request = capturedRequest();
        assertThat(request.memo()).isEqualTo("점심");
        assertThat(request.location().latitude()).isEqualTo(37.5);
    }

    @Test
    void rejectsMalformedRequestPart() {
        assertThatThrownBy(() -> controller.createContexts(1L, 10L, "{\"memo\":", null))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_HTTP_BODY);
    }

    private DiaryContextCreateRequest capturedRequest() {
        ArgumentCaptor<DiaryContextCreateRequest> captor =
                ArgumentCaptor.forClass(DiaryContextCreateRequest.class);
        org.mockito.Mockito.verify(diaryContextService)
                .createContexts(eq(1L), eq(10L), captor.capture(), any());
        return captor.getValue();
    }
}
