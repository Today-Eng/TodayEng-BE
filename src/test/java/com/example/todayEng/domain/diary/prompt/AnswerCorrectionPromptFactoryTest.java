package com.example.todayEng.domain.diary.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.dto.llm.AnswerCorrectionCommand;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AnswerCorrectionPromptFactoryTest {
    private final AnswerCorrectionPromptFactory factory = new AnswerCorrectionPromptFactory();

    @Test
    void mainPromptRequiresGroundedFollowUpAndMeaningPreservation() throws Exception {
        String prompt = factory.create(new AnswerCorrectionCommand(QuestionType.MAIN, "How was it?",
                "It good", EnglishLevel.BEGINNER,
                new ObjectMapper().readTree("{\"event\":\"walk\"}")));
        assertThat(prompt).contains("preserving its original meaning", "Never invent or add facts",
                "exactly one natural follow-up", "BEGINNER", "walk");
    }

    @Test
    void followUpPromptProhibitsAnotherFollowUp() {
        String prompt = factory.create(new AnswerCorrectionCommand(QuestionType.FOLLOW_UP, "Why?",
                "Because fun", EnglishLevel.INTERMEDIATE, null));
        assertThat(prompt).contains("followUpQuestion must be null");
    }
}
