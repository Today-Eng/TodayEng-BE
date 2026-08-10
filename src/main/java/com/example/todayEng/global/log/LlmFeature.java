package com.example.todayEng.global.log;

/**
 * 여러 기능이 같은 API 키와 모델을 공유하므로, 실패 로그에서 호출 경로를 구분하기 위한 식별자.
 */
public enum LlmFeature {

    REFLECTION_QUESTION("reflection-question"),
    ANSWER_CORRECTION("answer-correction"),
    DIARY_MEMORY_ANALYSIS("diary-memory-analysis"),
    DIARY_IMAGE_ANALYSIS("diary-image-analysis");

    private final String value;

    LlmFeature(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
