package com.example.todayEng.domain.diary.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class DiaryQuestionTest {

    @Test
    void followUpInheritsParentContextAndSnapshots() {
        Diary diary = mock(Diary.class);
        DiaryQuestion parent = mock(DiaryQuestion.class);
        DiaryContext context = mock(DiaryContext.class);
        var interests = new ObjectMapper().createArrayNode().add("BOOK");
        when(parent.getQuestionType()).thenReturn(QuestionType.MAIN);
        when(parent.getQuestionOrder()).thenReturn(1);
        when(parent.getContext()).thenReturn(context);
        when(parent.getEnglishLevelSnapshot()).thenReturn(EnglishLevel.BEGINNER);
        when(parent.getInterestSnapshot()).thenReturn(interests);

        DiaryQuestion followUp = DiaryQuestion.createFollowUpQuestion(
                diary, parent, 2, "Why?", "왜인가요?");

        assertThat(followUp.getQuestionType()).isEqualTo(QuestionType.FOLLOW_UP);
        assertThat(followUp.getQuestionOrder()).isEqualTo(2);
        assertThat(followUp.getContext()).isSameAs(context);
        assertThat(followUp.getEnglishLevelSnapshot()).isEqualTo(EnglishLevel.BEGINNER);
        assertThat(followUp.getInterestSnapshot()).isSameAs(interests);
    }
}
