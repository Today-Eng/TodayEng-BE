package com.example.todayEng.domain.diary.dto.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReflectionQuestionGenerationCommandTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("컨텍스트가 3개 이상이면 세 질문 모두 서로 다른 컨텍스트를 사용한다")
    void plansThreeDistinctContextQuestions() {
        var plan = command(List.of("MUSIC"), 3).plan();

        assertThat(plan.contextQuestionCount()).isEqualTo(3);
        assertThat(plan.interestQuestionCount()).isZero();
        assertThat(plan.requireDistinctContexts()).isTrue();
    }

    @Test
    @DisplayName("컨텍스트가 4개여도 질문은 세 개이므로 컨텍스트 기반 질문은 3개로 제한된다")
    void capsContextQuestionsAtTotalQuestionCount() {
        var plan = command(List.of("MUSIC"), 4).plan();

        assertThat(plan.contextQuestionCount()).isEqualTo(3);
        assertThat(plan.interestQuestionCount()).isZero();
    }

    @Test
    @DisplayName("컨텍스트가 2개면 컨텍스트 질문 2개와 관심사 질문 1개로 나눈다")
    void plansTwoContextQuestionsAndOneInterestQuestion() {
        var plan = command(List.of("MUSIC"), 2).plan();

        assertThat(plan.contextQuestionCount()).isEqualTo(2);
        assertThat(plan.interestQuestionCount()).isEqualTo(1);
        assertThat(plan.requireDistinctContexts()).isTrue();
    }

    @Test
    @DisplayName("컨텍스트가 1개면 컨텍스트 질문 1개와 관심사 질문 2개로 나눈다")
    void plansOneContextQuestionAndTwoInterestQuestions() {
        var plan = command(List.of("MUSIC"), 1).plan();

        assertThat(plan.contextQuestionCount()).isEqualTo(1);
        assertThat(plan.interestQuestionCount()).isEqualTo(2);
        assertThat(plan.requireDistinctContexts()).isTrue();
    }

    @Test
    @DisplayName("컨텍스트가 없으면 세 질문 모두 관심사 기반으로 생성한다")
    void plansThreeInterestQuestionsWithoutContext() {
        var plan = command(List.of("MUSIC"), 0).plan();

        assertThat(plan.contextQuestionCount()).isZero();
        assertThat(plan.interestQuestionCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("관심사가 없으면 대체 소재가 없으므로 컨텍스트 반복을 허용한다")
    void allowsContextReuseWhenUserHasNoInterests() {
        var plan = command(List.of(), 1).plan();

        assertThat(plan.contextQuestionCount()).isEqualTo(3);
        assertThat(plan.interestQuestionCount()).isZero();
        assertThat(plan.requireDistinctContexts()).isFalse();
    }

    private ReflectionQuestionGenerationCommand command(
            List<String> interests,
            int contextCount
    ) {
        List<ReflectionQuestionGenerationCommand.ContextInput> contexts =
                IntStream.rangeClosed(1, contextCount)
                        .mapToObj(index ->
                                new ReflectionQuestionGenerationCommand.ContextInput(
                                        (long) index,
                                        DiaryContextType.MEMO,
                                        objectMapper.createObjectNode()
                                                .put("memo", "context " + index)
                                ))
                        .toList();

        return new ReflectionQuestionGenerationCommand(
                1L, 10L, 1L, "성연", EnglishLevel.INTERMEDIATE, interests, contexts);
    }
}
