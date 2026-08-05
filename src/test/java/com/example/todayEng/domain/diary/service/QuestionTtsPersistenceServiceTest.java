package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.entity.enums.TtsStatus;
import com.example.todayEng.domain.diary.repository.DiaryQuestionRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QuestionTtsPersistenceServiceTest {

    @Mock private DiaryQuestionRepository questionRepository;
    private QuestionTtsPersistenceService service;
    private DiaryQuestion question;

    @BeforeEach
    void setUp() {
        service = new QuestionTtsPersistenceService(questionRepository);
        User user = User.create();
        ReflectionTestUtils.setField(user, "id", 1L);
        Diary diary = Diary.create(user, LocalDate.now());
        ReflectionTestUtils.setField(diary, "id", 10L);
        DiaryContext context = DiaryContext.success(
                diary,
                DiaryContextType.MEMO,
                new ObjectMapper().createObjectNode().put("memo", "worked")
        );
        question = DiaryQuestion.createGeneratedMainQuestion(
                diary,
                context,
                1,
                "How was your day?",
                "오늘은 어땠나요?",
                "day",
                EnglishLevel.BEGINNER,
                new ObjectMapper().createArrayNode()
        );
        ReflectionTestUtils.setField(question, "id", 101L);
    }

    @Test
    void claimsPendingOrFailedQuestionForGeneration() {
        given(questionRepository.claimTtsGeneration(1L, 10L, 101L))
                .willReturn(1);
        given(questionRepository.findByIdAndDiaryIdAndDiaryUserId(
                101L, 10L, 1L
        )).willReturn(Optional.of(question));

        var command = service.claim(1L, 10L, 101L);

        assertThat(command.questionText()).isEqualTo("How was your day?");
    }

    @Test
    void rejectsQuestionWhoseTtsIsAlreadyCompletedOrProcessing() {
        given(questionRepository.claimTtsGeneration(1L, 10L, 101L))
                .willReturn(0);
        given(questionRepository.findByIdAndDiaryIdAndDiaryUserId(
                101L, 10L, 1L
        )).willReturn(Optional.of(question));

        assertThatThrownBy(() -> service.claim(1L, 10L, 101L))
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.QUESTION_TTS_ALREADY_PROCESSING);
    }

    @Test
    void storesSuccessAndFailureState() {
        var command = new com.example.todayEng.domain.diary.dto.tts.QuestionTtsCommand(
                1L, 10L, 101L, "How was your day?"
        );
        given(questionRepository.findByIdAndDiaryIdAndDiaryUserId(
                101L, 10L, 1L
        )).willReturn(Optional.of(question));

        service.complete(command, "question.mp3");

        assertThat(question.getTtsStatus()).isEqualTo(TtsStatus.SUCCEEDED);
        assertThat(question.getTtsAudioKey()).isEqualTo("question.mp3");

        service.fail(command, new RuntimeException("failed\nsecret"));

        assertThat(question.getTtsStatus()).isEqualTo(TtsStatus.FAILED);
        assertThat(question.getTtsAudioKey()).isNull();
        assertThat(question.getTtsErrorMessage()).isEqualTo("failed secret");
    }
}
