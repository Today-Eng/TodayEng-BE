package com.example.todayEng.domain.diary.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.todayEng.domain.diary.dto.response.*;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.entity.enums.ReflectionQuestionGenerationStatus;
import com.example.todayEng.domain.diary.service.DiaryQuestionQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiaryQuestionControllerTest {
    @Mock DiaryQuestionQueryService service;
    @InjectMocks DiaryQuestionController controller;

    @Test void returnsQuestionListApiResponse() {
        var data = new DiaryQuestionListResponse(2L, DiaryStatus.IN_PROGRESS,
                ReflectionQuestionGenerationStatus.COMPLETED, List.of());
        when(service.getQuestions(1L, 2L)).thenReturn(data);
        var response = controller.getQuestions(1L, 2L);
        assertThat(response.getMessage()).isEqualTo("회고 질문 목록을 조회했습니다.");
        assertThat(response.getData()).isSameAs(data);
    }

    @Test void returnsNextQuestionApiResponse() {
        var data = new NextDiaryQuestionResponse(NextQuestionStatus.WAITING, null);
        when(service.getNextQuestion(1L, 2L)).thenReturn(data);
        var response = controller.getNextQuestion(1L, 2L);
        assertThat(response.getMessage()).isEqualTo("현재 회고 질문을 조회했습니다.");
        assertThat(response.getData().status()).isEqualTo(NextQuestionStatus.WAITING);
    }
}
