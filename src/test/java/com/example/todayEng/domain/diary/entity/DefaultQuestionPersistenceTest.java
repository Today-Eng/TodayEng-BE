package com.example.todayEng.domain.diary.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class DefaultQuestionPersistenceTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void rejectsDuplicateQuestionCode() {
        entityManager.persist(DefaultQuestion.createFollowUp(
                "FOLLOW_UP_01", null, "Could you tell me more?", "Korean translation"));
        entityManager.flush();

        assertThatThrownBy(() -> {
            entityManager.persist(DefaultQuestion.createFollowUp(
                    "FOLLOW_UP_01", null, "How did you feel?", "Korean translation"));
            entityManager.flush();
        })
                .isInstanceOf(PersistenceException.class);
    }
}
