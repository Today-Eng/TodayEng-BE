package com.example.todayEng.domain.diary.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.user.entity.InterestTag;
import com.example.todayEng.domain.user.entity.enums.InterestTagName;
import org.junit.jupiter.api.Test;

class DefaultQuestionTest {

    private final InterestTag music = InterestTag.create(InterestTagName.MUSIC);

    @Test
    void createsMainQuestionWithInterestTag() {
        DefaultQuestion question = DefaultQuestion.createMain(
                "MAIN_MUSIC_01", music, "What did you listen to?", "Korean translation");

        assertThat(question.getQuestionCode()).isEqualTo("MAIN_MUSIC_01");
        assertThat(question.getQuestionType()).isEqualTo(QuestionType.MAIN);
        assertThat(question.getInterestTag()).isEqualTo(music);
    }

    @Test
    void rejectsMainQuestionWithoutInterestTag() {
        assertThatThrownBy(() -> DefaultQuestion.createMain(
                "MAIN_DAILY_01", null, "How was your day?", "Korean translation"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsQuestionWithoutCode() {
        assertThatThrownBy(() -> DefaultQuestion.createMain(
                " ", music, "How was your day?", "Korean translation"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createsGenericFollowUpWithoutInterestTag() {
        DefaultQuestion question = DefaultQuestion.createFollowUp(
                "FOLLOW_UP_01", null, "Could you tell me more?", "Korean translation");

        assertThat(question.getQuestionCode()).isEqualTo("FOLLOW_UP_01");
        assertThat(question.getQuestionType()).isEqualTo(QuestionType.FOLLOW_UP);
        assertThat(question.getInterestTag()).isNull();
    }

    @Test
    void createsInterestSpecificFollowUp() {
        DefaultQuestion question = DefaultQuestion.createFollowUp(
                "FOLLOW_UP_MUSIC_01", music, "Why was it meaningful?", "Korean translation");

        assertThat(question.getQuestionType()).isEqualTo(QuestionType.FOLLOW_UP);
        assertThat(question.getInterestTag()).isEqualTo(music);
    }
}
