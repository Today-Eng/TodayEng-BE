package com.example.todayEng.domain.diary.sse;

public enum DiarySseEventType {
    CONNECTED,
    HEARTBEAT,
    QUESTION_READY,
    QUESTIONS_READY("questions.ready"),
    ANSWER_TRANSCRIBED,
    ANSWER_CORRECTED,
    PROCESSING_FAILED;

    private final String eventName;

    DiarySseEventType() {
        this.eventName = name().toLowerCase();
    }

    DiarySseEventType(String eventName) {
        this.eventName = eventName;
    }

    public String eventName() {
        return eventName;
    }
}
