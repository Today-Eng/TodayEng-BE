package com.example.todayEng.domain.diary.sse;

public enum DiarySseEventType {
    CONNECTED,
    HEARTBEAT,
    QUESTION_READY("question.ready"),
    QUESTIONS_READY("questions.ready"),
    ANSWER_TRANSCRIBED("answer.transcribed"),
    ANSWER_CORRECTED("answer.corrected"),
    FOLLOW_UP_READY("follow-up.ready"),
    READY_TO_COMPLETE("ready-to-complete"),
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
