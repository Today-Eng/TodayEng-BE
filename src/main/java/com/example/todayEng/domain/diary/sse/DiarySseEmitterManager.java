package com.example.todayEng.domain.diary.sse;

import com.example.todayEng.domain.diary.dto.sse.DiarySseEvent;
import com.example.todayEng.domain.diary.dto.sse.DiarySsePayload;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class DiarySseEmitterManager {

    private final Map<EmitterKey, SseEmitter> emitters =
            new ConcurrentHashMap<>();
    private final DiarySseEmitterFactory emitterFactory;
    private final long timeoutMillis;

    public DiarySseEmitterManager(
            DiarySseEmitterFactory emitterFactory,
            @Value("${diary.sse.timeout-millis:1800000}") long timeoutMillis
    ) {
        this.emitterFactory = emitterFactory;
        this.timeoutMillis = timeoutMillis;
    }

    public SseEmitter subscribe(Long userId, Long diaryId) {
        EmitterKey key = new EmitterKey(userId, diaryId);
        SseEmitter emitter = emitterFactory.create(timeoutMillis);
        SseEmitter previousEmitter = emitters.put(key, emitter);

        if (previousEmitter != null) {
            completeQuietly(previousEmitter);
        }

        registerCallbacks(key, emitter);
        sendToEmitter(
                key,
                emitter,
                DiarySseEventType.CONNECTED,
                UUID.randomUUID().toString(),
                new DiarySsePayload.Connected("SSE connection established")
        );
        return emitter;
    }

    public void sendQuestionReady(
            Long userId,
            Long diaryId,
            DiarySsePayload.QuestionReady data
    ) {
        sendIfPresent(userId, diaryId, DiarySseEventType.QUESTION_READY, data);
    }

    public void sendQuestionsReady(
            Long userId,
            Long diaryId,
            DiarySsePayload.QuestionsReady data
    ) {
        sendIfPresent(userId, diaryId, DiarySseEventType.QUESTIONS_READY, data);
    }

    public void sendAnswerTranscribed(
            Long userId,
            Long diaryId,
            DiarySsePayload.AnswerTranscribed data
    ) {
        sendIfPresent(userId, diaryId, DiarySseEventType.ANSWER_TRANSCRIBED, data);
    }

    public void sendAnswerCorrected(
            Long userId,
            Long diaryId,
            DiarySsePayload.AnswerCorrected data
    ) {
        sendIfPresent(userId, diaryId, DiarySseEventType.ANSWER_CORRECTED, data);
    }

    public void sendProcessingFailed(
            Long userId,
            Long diaryId,
            DiarySsePayload.ProcessingFailed data
    ) {
        sendIfPresent(userId, diaryId, DiarySseEventType.PROCESSING_FAILED, data);
    }

    @Scheduled(fixedDelayString = "${diary.sse.heartbeat-interval-millis:30000}")
    public void sendHeartbeat() {
        emitters.forEach((key, emitter) -> sendToEmitter(
                key,
                emitter,
                DiarySseEventType.HEARTBEAT,
                UUID.randomUUID().toString(),
                new DiarySsePayload.Heartbeat("ping")
        ));
    }

    int emitterCount() {
        return emitters.size();
    }

    boolean contains(Long userId, Long diaryId) {
        return emitters.containsKey(new EmitterKey(userId, diaryId));
    }

    private void sendIfPresent(
            Long userId,
            Long diaryId,
            DiarySseEventType type,
            Object data
    ) {
        EmitterKey key = new EmitterKey(userId, diaryId);
        SseEmitter emitter = emitters.get(key);

        if (emitter == null) {
            log.debug(
                    "SSE emitter is absent: userId={}, diaryId={}, event={}",
                    userId,
                    diaryId,
                    type
            );
            return;
        }

        sendToEmitter(
                key,
                emitter,
                type,
                createBusinessEventId(type, key, data),
                data
        );
    }

    private void sendToEmitter(
            EmitterKey key,
            SseEmitter emitter,
            DiarySseEventType type,
            String eventId,
            Object data
    ) {
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name(type.eventName())
                    .data(DiarySseEvent.of(
                            type.eventName(),
                            key.diaryId(),
                            data
                    )));
        } catch (IOException | RuntimeException exception) {
            remove(key, emitter);
            completeWithErrorQuietly(emitter, exception);
            log.debug(
                    "SSE send failed: userId={}, diaryId={}, event={}",
                    key.userId(),
                    key.diaryId(),
                    type
            );
        }
    }

    private String createBusinessEventId(
            DiarySseEventType type,
            EmitterKey key,
            Object data
    ) {
        String resourceId;
        if (data instanceof DiarySsePayload.QuestionReady questionReady) {
            resourceId = String.valueOf(questionReady.questionId());
        } else if (data instanceof DiarySsePayload.QuestionsReady) {
            resourceId = "main";
        } else if (data instanceof DiarySsePayload.AnswerTranscribed answerTranscribed) {
            resourceId = String.valueOf(answerTranscribed.answerId());
        } else if (data instanceof DiarySsePayload.AnswerCorrected answerCorrected) {
            resourceId = String.valueOf(answerCorrected.answerId());
        } else if (data instanceof DiarySsePayload.ProcessingFailed processingFailed) {
            resourceId = processingFailed.stage()
                    + ":" + processingFailed.errorCode();
        } else {
            resourceId = UUID.randomUUID().toString();
        }

        return type.eventName()
                + ":" + key.diaryId()
                + ":" + resourceId;
    }

    private void registerCallbacks(
            EmitterKey key,
            SseEmitter emitter
    ) {
        emitter.onCompletion(() -> remove(key, emitter));
        emitter.onTimeout(() -> {
            remove(key, emitter);
            completeQuietly(emitter);
        });
        emitter.onError(exception -> remove(key, emitter));
    }

    private void remove(EmitterKey key, SseEmitter expectedEmitter) {
        emitters.remove(key, expectedEmitter);
    }

    private void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // The connection was already completed by the servlet container.
        }
    }

    private void completeWithErrorQuietly(
            SseEmitter emitter,
            Throwable throwable
    ) {
        try {
            emitter.completeWithError(throwable);
        } catch (IllegalStateException ignored) {
            // The connection was already completed by the servlet container.
        }
    }

    private record EmitterKey(Long userId, Long diaryId) {
    }
}
