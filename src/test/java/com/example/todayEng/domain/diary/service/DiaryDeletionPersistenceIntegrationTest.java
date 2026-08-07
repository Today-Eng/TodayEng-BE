package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(DiaryDeletionService.class)
class DiaryDeletionPersistenceIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired DiaryRepository diaryRepository;
    @Autowired DiaryDeletionService diaryDeletionService;
    @Autowired EntityManager entityManager;

    @Test
    void persistsDeletedStatusAfterPersistenceContextClear() {
        User user = userRepository.save(User.create());
        Diary diary = Diary.create(user, java.time.LocalDate.of(2026, 8, 7));
        diary.complete("final memo");
        diary = diaryRepository.saveAndFlush(diary);

        diaryDeletionService.delete(user.getId(), diary.getId());

        entityManager.flush();
        entityManager.clear();

        Diary reloaded = diaryRepository.findById(diary.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DiaryStatus.DELETED);
        assertThat(reloaded.getMemo()).isNull();
    }
}
