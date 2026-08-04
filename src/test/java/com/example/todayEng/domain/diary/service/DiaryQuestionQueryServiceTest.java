package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.example.todayEng.domain.diary.dto.response.NextQuestionStatus;
import com.example.todayEng.domain.diary.entity.*;
import com.example.todayEng.domain.diary.entity.enums.QuestionGenerationType;
import com.example.todayEng.domain.diary.repository.*;
import com.example.todayEng.domain.diary.storage.AudioFileStorage;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DiaryQuestionQueryServiceTest {
    @Mock DiaryRepository diaryRepository;
    @Mock DiaryQuestionRepository questionRepository;
    @Mock DiaryAnswerRepository answerRepository;
    @Mock AudioFileStorage audioFileStorage;
    @InjectMocks DiaryQuestionQueryService service;
    Diary diary;

    @BeforeEach void setUp() {
        User user = User.create(); ReflectionTestUtils.setField(user, "id", 1L);
        diary = Diary.create(user, LocalDate.now()); ReflectionTestUtils.setField(diary, "id", 2L);
        when(diaryRepository.findById(2L)).thenReturn(Optional.of(diary));
    }

    @Test void listsQuestionsWithProgressUsingTwoBulkQueries() {
        DiaryQuestion question = question(10L, 1, true);
        DiaryAnswer answer = correctedAnswer(20L, question);
        when(questionRepository.findAllByDiaryIdInReflectionOrder(2L)).thenReturn(List.of(question));
        when(answerRepository.findAllByDiaryIdInReflectionOrder(2L)).thenReturn(List.of(answer));
        when(audioFileStorage.publicUrl("audio-key")).thenReturn("/audio/audio-key");

        var result = service.getQuestions(1L, 2L);

        assertThat(result.questions()).hasSize(1);
        assertThat(result.questions().get(0).answerId()).isEqualTo(20L);
        assertThat(result.questions().get(0).ttsAudioUrl()).isEqualTo("/audio/audio-key");
        verify(questionRepository, times(1)).findAllByDiaryIdInReflectionOrder(2L);
        verify(answerRepository, times(1)).findAllByDiaryIdInReflectionOrder(2L);
    }

    @Test void returnsFirstSequentialReadyQuestion() {
        DiaryQuestion first = question(10L, 1, true);
        DiaryQuestion second = question(11L, 3, true);
        when(questionRepository.findAllByDiaryIdInReflectionOrder(2L)).thenReturn(List.of(first, second));
        when(answerRepository.findAllByDiaryIdInReflectionOrder(2L))
                .thenReturn(List.of(correctedAnswer(20L, first)));

        var result = service.getNextQuestion(1L, 2L);

        assertThat(result.status()).isEqualTo(NextQuestionStatus.QUESTION_READY);
        assertThat(result.question().questionId()).isEqualTo(11L);
    }

    @Test void waitsWhileEarlierAnswerCorrectionIsNotComplete() {
        DiaryQuestion first = question(10L, 1, true);
        DiaryQuestion second = question(11L, 3, true);
        DiaryAnswer pending = DiaryAnswer.create(first, "answer");
        ReflectionTestUtils.setField(pending, "id", 20L);
        when(questionRepository.findAllByDiaryIdInReflectionOrder(2L)).thenReturn(List.of(first, second));
        when(answerRepository.findAllByDiaryIdInReflectionOrder(2L)).thenReturn(List.of(pending));

        assertThat(service.getNextQuestion(1L, 2L).status()).isEqualTo(NextQuestionStatus.WAITING);
    }

    @Test void returnsReadyToCompleteAfterSixCorrectedAnswers() {
        List<DiaryQuestion> questions = new ArrayList<>();
        List<DiaryAnswer> answers = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            DiaryQuestion q = question((long) i, i, true);
            questions.add(q); answers.add(correctedAnswer((long) i + 10, q));
        }
        when(questionRepository.findAllByDiaryIdInReflectionOrder(2L)).thenReturn(questions);
        when(answerRepository.findAllByDiaryIdInReflectionOrder(2L)).thenReturn(answers);

        assertThat(service.getNextQuestion(1L, 2L).status()).isEqualTo(NextQuestionStatus.READY_TO_COMPLETE);
    }

    @Test void rejectsOtherOwner() {
        assertThatThrownBy(() -> service.getQuestions(9L, 2L))
                .isInstanceOfSatisfying(BaseException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
        verifyNoInteractions(questionRepository, answerRepository);
    }

    private DiaryQuestion question(Long id, int order, boolean ttsReady) {
        DiaryQuestion q = DiaryQuestion.createMainQuestion(diary, order, "question", QuestionGenerationType.AI,
                "질문", null, null);
        ReflectionTestUtils.setField(q, "id", id);
        if (ttsReady) q.completeTts("audio-key");
        return q;
    }

    private DiaryAnswer correctedAnswer(Long id, DiaryQuestion question) {
        DiaryAnswer answer = DiaryAnswer.create(question, "answer");
        ReflectionTestUtils.setField(answer, "id", id);
        answer.completeCorrection("corrected", "reason", new ObjectMapper().createArrayNode());
        return answer;
    }
}
