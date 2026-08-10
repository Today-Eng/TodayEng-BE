package com.example.todayEng.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.QuestionGenerationType;
import com.example.todayEng.domain.diary.repository.DiaryAnswerRepository;
import com.example.todayEng.domain.diary.repository.DiaryQuestionRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.diary.service.DiaryDeletionService;
import com.example.todayEng.domain.home.entity.DailyContextSnapshot;
import com.example.todayEng.domain.home.repository.DailyContextSnapshotRepository;
import com.example.todayEng.domain.user.entity.OAuthAuthorizationRequest;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.OAuthAuthorizationRequestRepository;
import com.example.todayEng.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({UserService.class, DiaryDeletionService.class})
class UserWithdrawalPersistenceIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired DiaryRepository diaryRepository;
    @Autowired DiaryQuestionRepository questionRepository;
    @Autowired DiaryAnswerRepository answerRepository;
    @Autowired DailyContextSnapshotRepository snapshotRepository;
    @Autowired OAuthAuthorizationRequestRepository authorizationRequestRepository;
    @Autowired UserService userService;
    @Autowired EntityManager entityManager;

    @Test
    void deletesUserAndAllReferencingData() {
        User user = userRepository.saveAndFlush(User.create("withdraw@example.com"));

        Diary diary = diaryRepository.saveAndFlush(
                Diary.create(user, LocalDate.of(2026, 8, 10))
        );
        DiaryQuestion question = questionRepository.saveAndFlush(
                DiaryQuestion.createMainQuestion(
                        diary,
                        1,
                        "How was your day?",
                        QuestionGenerationType.AI,
                        "오늘 하루는 어땠나요?",
                        "day",
                        null
                )
        );
        answerRepository.saveAndFlush(DiaryAnswer.create(question, "It was good."));

        snapshotRepository.saveAndFlush(
                DailyContextSnapshot.start(
                        user,
                        LocalDate.of(2026, 8, 10),
                        com.example.todayEng.domain.diary.entity.enums.DiaryContextType.WEATHER
                )
        );
        authorizationRequestRepository.saveAndFlush(
                OAuthAuthorizationRequest.create(
                        user,
                        ExternalServiceProvider.GOOGLE_CALENDAR,
                        "state-hash",
                        LocalDateTime.now().plusMinutes(10)
                )
        );

        userService.withdraw(user.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(user.getId())).isEmpty();
        assertThat(diaryRepository.count()).isZero();
        assertThat(questionRepository.count()).isZero();
        assertThat(answerRepository.count()).isZero();
        assertThat(snapshotRepository.count()).isZero();
        assertThat(authorizationRequestRepository.count()).isZero();
    }
}
