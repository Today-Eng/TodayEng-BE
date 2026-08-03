package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todayEng.domain.diary.entity.*;
import com.example.todayEng.domain.diary.entity.enums.QuestionGenerationType;
import com.example.todayEng.domain.diary.repository.DiaryAnswerRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiaryCompletionPolicyTest {
    private final DiaryCompletionPolicy policy = new DiaryCompletionPolicy();

    @Test void acceptsSixOrderedCorrectedAnswers() {
        Fixture f = fixture();
        assertThatCode(() -> policy.validate(f.questions, f.answers)).doesNotThrowAnyException();
    }

    @Test void rejectsWrongMainCount() {
        Fixture f = fixture();
        f.questions.remove(0);
        assertError(() -> policy.validate(f.questions, f.answers), ErrorCode.INVALID_REFLECTION_QUESTION_COMPOSITION);
    }

    @Test void rejectsWrongFollowUpCount() {
        Fixture f = fixture();
        f.questions.remove(1);
        assertError(() -> policy.validate(f.questions, f.answers), ErrorCode.INVALID_REFLECTION_QUESTION_COMPOSITION);
    }

    @Test void rejectsMissingAnswer() {
        Fixture f = fixture();
        f.answers.remove(0);
        assertError(() -> policy.validate(f.questions, f.answers), ErrorCode.INCOMPLETE_REFLECTION_ANSWERS);
    }

    @Test void rejectsBlankOriginalText() {
        Fixture f = fixture();
        f.answers.set(0, DiaryAnswer.create(f.questions.get(0), " "));
        f.answers.get(0).completeCorrection("x", "r", new ObjectMapper().createArrayNode());
        assertError(() -> policy.validate(f.questions, f.answers), ErrorCode.INCOMPLETE_REFLECTION_ANSWERS);
    }

    @Test void rejectsPendingCorrection() {
        Fixture f = fixture();
        f.answers.set(0, DiaryAnswer.create(f.questions.get(0), "answer"));
        assertError(() -> policy.validate(f.questions, f.answers), ErrorCode.REFLECTION_CORRECTION_NOT_FINISHED);
    }

    @Test void rejectsFailedCorrection() {
        Fixture f = fixture();
        f.answers.get(0).failCorrection();
        assertError(() -> policy.validate(f.questions, f.answers), ErrorCode.REFLECTION_CORRECTION_NOT_FINISHED);
    }

    private Fixture fixture() {
        Diary diary = Diary.create(com.example.todayEng.domain.user.entity.User.create(), LocalDate.now());
        List<DiaryQuestion> questions = new ArrayList<>();
        List<DiaryAnswer> answers = new ArrayList<>();
        for (int order = 1; order <= 5; order += 2) {
            DiaryQuestion main = DiaryQuestion.createMainQuestion(diary, order, "main", QuestionGenerationType.AI,
                    "메인", null, null);
            DiaryQuestion follow = DiaryQuestion.createFollowUpQuestion(diary, main, "follow", "후속");
            questions.add(main); questions.add(follow);
        }
        for (DiaryQuestion q : questions) {
            DiaryAnswer a = DiaryAnswer.create(q, "answer");
            a.completeCorrection("corrected", "reason", new ObjectMapper().createArrayNode());
            answers.add(a);
        }
        return new Fixture(questions, answers);
    }

    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BaseException.class,
                e -> org.assertj.core.api.Assertions.assertThat(e.getErrorCode()).isEqualTo(code));
    }

    private record Fixture(List<DiaryQuestion> questions, List<DiaryAnswer> answers) { }
}
