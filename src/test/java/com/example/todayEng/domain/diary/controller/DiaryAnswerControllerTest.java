package com.example.todayEng.domain.diary.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.todayEng.domain.diary.dto.response.AnswerUploadResponse;
import com.example.todayEng.domain.diary.entity.enums.TranscriptionStatus;
import com.example.todayEng.domain.diary.service.AnswerUploadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class DiaryAnswerControllerTest {
    @Mock AnswerUploadService service;
    @InjectMocks DiaryAnswerController controller;

    @Test
    void returnsAcceptedWithUploadedAnswer() {
        var audio = new MockMultipartFile("audio", "answer.webm", "audio/webm",
                new byte[]{0x1a, 0x45, (byte) 0xdf, (byte) 0xa3});
        when(service.upload(10L, 20L, 30L, audio))
                .thenReturn(new AnswerUploadResponse(40L, TranscriptionStatus.UPLOADED));

        var response = controller.upload(10L, 20L, 30L, audio);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("음성 답변 처리를 시작했습니다.");
        assertThat(response.getBody().getData().answerId()).isEqualTo(40L);
    }
}
