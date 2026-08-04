package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.diary.sse.DiarySseEmitterManager;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class DiarySubscriptionServiceTest {

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private DiarySseEmitterManager emitterManager;

    @InjectMocks
    private DiarySubscriptionService subscriptionService;

    @Test
    void ownerCanSubscribe() {
        SseEmitter emitter = new SseEmitter();
        given(diaryRepository.existsByIdAndUserId(10L, 1L))
                .willReturn(true);
        given(emitterManager.subscribe(1L, 10L))
                .willReturn(emitter);

        SseEmitter result = subscriptionService.subscribe(1L, 10L);

        assertThat(result).isSameAs(emitter);
    }

    @Test
    void nonOwnerCannotSubscribe() {
        given(diaryRepository.existsByIdAndUserId(10L, 2L))
                .willReturn(false);

        assertThatThrownBy(() ->
                subscriptionService.subscribe(2L, 10L)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        verify(emitterManager, never()).subscribe(2L, 10L);
    }
}
