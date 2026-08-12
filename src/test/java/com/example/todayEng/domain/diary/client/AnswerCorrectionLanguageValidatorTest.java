package com.example.todayEng.domain.diary.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AnswerCorrectionLanguageValidatorTest {

    private final AnswerCorrectionLanguageValidator validator = new AnswerCorrectionLanguageValidator();

    @Test
    void acceptsExpectedLanguages() {
        assertThat(validator.isEnglishText("It was a good day.")).isTrue();
        assertThat(validator.isKoreanExplanation("과거형에 맞게 표현을 수정했어요.")).isTrue();
        assertThat(validator.areEnglishExpressions(List.of("I had a good day.", "It was enjoyable."))).isTrue();
        assertThat(validator.areEnglishExpressions(List.of())).isTrue();
    }

    @Test
    void rejectsLanguageContractViolations() {
        assertThat(validator.isEnglishText("좋은 하루였어요.")).isFalse();
        assertThat(validator.isEnglishText("It was 좋은 day.")).isFalse();
        assertThat(validator.isKoreanExplanation("Changed the sentence to past tense.")).isFalse();
        assertThat(validator.areEnglishExpressions(List.of("I had fun.", "즐거웠어요."))).isFalse();
    }
}
