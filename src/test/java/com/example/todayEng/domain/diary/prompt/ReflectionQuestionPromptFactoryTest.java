package com.example.todayEng.domain.diary.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReflectionQuestionPromptFactoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReflectionQuestionPromptFactory promptFactory =
            new ReflectionQuestionPromptFactory(objectMapper);

    @Test
    void containsGroundingDifficultyAndInterestRules() {
        var command = new ReflectionQuestionGenerationCommand(
                1L,
                10L,
                1L,
                "성연",
                EnglishLevel.INTERMEDIATE,
                List.of("MUSIC"),
                List.of(new ReflectionQuestionGenerationCommand.ContextInput(
                        100L,
                        DiaryContextType.MEMO,
                        objectMapper.createObjectNode().put("memo", "Had lunch")
                ))
        );

        String prompt = promptFactory.create(command);

        assertThat(prompt)
                .contains("exactly three")
                .contains("Never invent")
                .contains("exactly one context")
                .contains("INTERMEDIATE")
                .contains("성연")
                .contains("only when it makes the question feel natural")
                .contains("untrusted reference data")
                .contains("MUSIC")
                .contains("Had lunch")
                .contains("contextId");
    }

    @Test
    void createsNonInventingInterestFallbackPromptWithoutContext() {
        var command = new ReflectionQuestionGenerationCommand(
                1L, 10L, 1L, "성연", EnglishLevel.BEGINNER,
                List.of("MUSIC", "TRAVEL"), List.of());

        String prompt = promptFactory.create(command);

        assertThat(prompt)
                .contains("There is no diary context")
                .contains("Never claim or imply")
                .contains("contextId as null")
                .contains("MUSIC", "TRAVEL", "BEGINNER");
    }
}
