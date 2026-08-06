package com.example.todayEng.domain.home.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.home.entity.DailyContextSnapshot;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class DailyContextSnapshotRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired DailyContextSnapshotRepository snapshotRepository;

    @Test
    void rejectsDuplicateUserDateAndContextType() {
        User user = userRepository.save(User.create());
        LocalDate date = LocalDate.of(2026, 8, 6);
        snapshotRepository.saveAndFlush(DailyContextSnapshot.start(
                user, date, DiaryContextType.WEATHER));

        assertThatThrownBy(() -> snapshotRepository.saveAndFlush(
                DailyContextSnapshot.start(
                        user, date, DiaryContextType.WEATHER
                )
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
