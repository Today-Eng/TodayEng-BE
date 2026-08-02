package com.example.todayEng.domain.diary.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class DiarySseEmitterFactory {

    public SseEmitter create(long timeoutMillis) {
        return new SseEmitter(timeoutMillis);
    }
}
