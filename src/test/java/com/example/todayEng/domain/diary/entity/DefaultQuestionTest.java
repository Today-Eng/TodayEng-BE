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
        DefaultQuestion question =
                DefaultQuestion.createMain(music, "What did you listen to?", "무엇을 들었나요?");

        assertThat(question.getQuestionType()).isEqualTo(QuestionType.MAIN);
        assertThat(question.getInterestTag()).isEqualTo(music);
    }

    @Test
    void rejectsMainQuestionWithoutInterestTag() {
        assertThatThrownBy(() ->
                DefaultQuestion.createMain(null, "How was your day?", "오늘 어땠나요?"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createsGenericFollowUpWithoutInterestTag() {
        DefaultQuestion question =
                DefaultQuestion.createFollowUp(null, "Could you tell me more?", "더 말해주시겠어요?");

        assertThat(question.getQuestionType()).isEqualTo(QuestionType.FOLLOW_UP);
        assertThat(question.getInterestTag()).isNull();
    }

    @Test
    void createsInterestSpecificFollowUp() {
        DefaultQuestion question =
                DefaultQuestion.createFollowUp(music, "Why was it meaningful?", "왜 의미 있었나요?");

        assertThat(question.getQuestionType()).isEqualTo(QuestionType.FOLLOW_UP);
        assertThat(question.getInterestTag()).isEqualTo(music);
    }
}
