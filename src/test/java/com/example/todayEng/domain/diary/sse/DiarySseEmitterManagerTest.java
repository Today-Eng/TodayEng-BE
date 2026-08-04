package com.example.todayEng.domain.diary.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.todayEng.domain.diary.dto.sse.DiarySsePayload;
import java.io.IOException;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class DiarySseEmitterManagerTest {

    private DiarySseEmitterFactory emitterFactory;
    private DiarySseEmitterManager emitterManager;

    @BeforeEach
    void setUp() {
        emitterFactory = mock(DiarySseEmitterFactory.class);
        emitterManager = new DiarySseEmitterManager(
                emitterFactory,
                30_000L
        );
    }

    @Test
    void sendsConnectedEventImmediately() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        when(emitterFactory.create(30_000L)).thenReturn(emitter);

        SseEmitter result = emitterManager.subscribe(1L, 10L);

        assertThat(result).isSameAs(emitter);
        assertThat(emitterManager.contains(1L, 10L)).isTrue();
        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void reconnectCompletesPreviousConnection() {
        SseEmitter previous = mock(SseEmitter.class);
        SseEmitter replacement = mock(SseEmitter.class);
        when(emitterFactory.create(30_000L))
                .thenReturn(previous, replacement);

        emitterManager.subscribe(1L, 10L);
        emitterManager.subscribe(1L, 10L);

        verify(previous).complete();
        assertThat(emitterManager.emitterCount()).isEqualTo(1);
        assertThat(emitterManager.contains(1L, 10L)).isTrue();
    }

    @Test
    void oldCompletionCallbackDoesNotRemoveReplacement() {
        SseEmitter previous = mock(SseEmitter.class);
        SseEmitter replacement = mock(SseEmitter.class);
        when(emitterFactory.create(30_000L))
                .thenReturn(previous, replacement);

        emitterManager.subscribe(1L, 10L);
        ArgumentCaptor<Runnable> completionCaptor =
                ArgumentCaptor.forClass(Runnable.class);
        verify(previous).onCompletion(completionCaptor.capture());

        emitterManager.subscribe(1L, 10L);
        completionCaptor.getValue().run();

        assertThat(emitterManager.contains(1L, 10L)).isTrue();
        assertThat(emitterManager.emitterCount()).isEqualTo(1);
    }

    @Test
    void completionRemovesCurrentEmitter() {
        SseEmitter emitter = mock(SseEmitter.class);
        when(emitterFactory.create(30_000L)).thenReturn(emitter);

        emitterManager.subscribe(1L, 10L);
        ArgumentCaptor<Runnable> completionCaptor =
                ArgumentCaptor.forClass(Runnable.class);
        verify(emitter).onCompletion(completionCaptor.capture());

        completionCaptor.getValue().run();

        assertThat(emitterManager.contains(1L, 10L)).isFalse();
    }

    @Test
    void timeoutRemovesAndCompletesCurrentEmitter() {
        SseEmitter emitter = mock(SseEmitter.class);
        when(emitterFactory.create(30_000L)).thenReturn(emitter);

        emitterManager.subscribe(1L, 10L);
        ArgumentCaptor<Runnable> timeoutCaptor =
                ArgumentCaptor.forClass(Runnable.class);
        verify(emitter).onTimeout(timeoutCaptor.capture());

        timeoutCaptor.getValue().run();

        assertThat(emitterManager.contains(1L, 10L)).isFalse();
        verify(emitter).complete();
    }

    @Test
    void errorCallbackRemovesCurrentEmitter() {
        SseEmitter emitter = mock(SseEmitter.class);
        when(emitterFactory.create(30_000L)).thenReturn(emitter);

        emitterManager.subscribe(1L, 10L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Throwable>> errorCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(emitter).onError(errorCaptor.capture());

        errorCaptor.getValue().accept(new IOException("connection closed"));

        assertThat(emitterManager.contains(1L, 10L)).isFalse();
    }

    @Test
    void absentEmitterDoesNotFailCaller() {
        DiarySsePayload.AnswerTranscribed data =
                new DiarySsePayload.AnswerTranscribed(
                        100L,
                        200L,
                        "Today was a good day."
                );

        assertThatCode(() ->
                emitterManager.sendAnswerTranscribed(1L, 10L, data)
        ).doesNotThrowAnyException();
    }

    @Test
    void failedSendRemovesEmitterWithoutPropagatingFailure()
            throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        when(emitterFactory.create(30_000L)).thenReturn(emitter);
        doNothing()
                .doThrow(new IOException("connection closed"))
                .when(emitter)
                .send(any(SseEmitter.SseEventBuilder.class));

        emitterManager.subscribe(1L, 10L);

        assertThatCode(() -> emitterManager.sendQuestionReady(
                1L,
                10L,
                new DiarySsePayload.QuestionReady(
                        100L,
                        "How was your day?",
                        "오늘 하루는 어땠나요?",
                        null
                )
        )).doesNotThrowAnyException();

        assertThat(emitterManager.contains(1L, 10L)).isFalse();
        verify(emitter).completeWithError(any(IOException.class));
    }

    @Test
    void runtimeSendFailureDoesNotPropagateToCaller()
            throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        when(emitterFactory.create(30_000L)).thenReturn(emitter);
        doNothing()
                .doThrow(new RuntimeException("serialization failed"))
                .when(emitter)
                .send(any(SseEmitter.SseEventBuilder.class));

        emitterManager.subscribe(1L, 10L);

        assertThatCode(() -> emitterManager.sendAnswerCorrected(
                1L,
                10L,
                new DiarySsePayload.AnswerCorrected(
                        100L,
                        200L,
                        "Today was a good day.",
                        "Grammar correction"
                )
        )).doesNotThrowAnyException();

        assertThat(emitterManager.contains(1L, 10L)).isFalse();
        verify(emitter).completeWithError(any(RuntimeException.class));
    }

    @Test
    void heartbeatIsSentToEveryConnectedEmitter()
            throws IOException {
        SseEmitter first = mock(SseEmitter.class);
        SseEmitter second = mock(SseEmitter.class);
        when(emitterFactory.create(30_000L))
                .thenReturn(first, second);

        emitterManager.subscribe(1L, 10L);
        emitterManager.subscribe(2L, 20L);
        emitterManager.sendHeartbeat();

        verify(first, times(2))
                .send(any(SseEmitter.SseEventBuilder.class));
        verify(second, times(2))
                .send(any(SseEmitter.SseEventBuilder.class));
    }
}
