package com.example.todayEng.domain.home.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.home.entity.DailyContextSnapshot;
import com.example.todayEng.domain.home.entity.enums.DailyContextCollectionStatus;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class DailyContextSnapshotRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired DailyContextSnapshotRepository snapshotRepository;
    @Autowired TestEntityManager entityManager;

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

    @Test
    void reclaimsInProgressSnapshotThatWentStale() {
        User user = userRepository.save(User.create());
        LocalDate date = LocalDate.of(2026, 8, 6);
        DailyContextSnapshot snapshot = snapshotRepository.saveAndFlush(
                DailyContextSnapshot.start(user, date, DiaryContextType.WEATHER));
        backdateUpdatedAt(snapshot.getId(), LocalDateTime.now().minusMinutes(10));

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        int updated = snapshotRepository.reclaimStale(
                user.getId(),
                date,
                DiaryContextType.WEATHER,
                DailyContextCollectionStatus.IN_PROGRESS,
                now,
                now.minusMinutes(5)
        );

        assertThat(updated).isEqualTo(1);
        DailyContextSnapshot reclaimed = snapshotRepository.findById(snapshot.getId())
                .orElseThrow();
        assertThat(reclaimed.getCollectionStatus())
                .isEqualTo(DailyContextCollectionStatus.IN_PROGRESS);
        assertThat(reclaimed.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void doesNotReclaimInProgressSnapshotThatIsStillActive() {
        User user = userRepository.save(User.create());
        LocalDate date = LocalDate.of(2026, 8, 6);
        DailyContextSnapshot snapshot = snapshotRepository.saveAndFlush(
                DailyContextSnapshot.start(user, date, DiaryContextType.WEATHER));
        backdateUpdatedAt(snapshot.getId(), LocalDateTime.now().minusMinutes(1));

        LocalDateTime now = LocalDateTime.now();
        int updated = snapshotRepository.reclaimStale(
                user.getId(),
                date,
                DiaryContextType.WEATHER,
                DailyContextCollectionStatus.IN_PROGRESS,
                now,
                now.minusMinutes(5)
        );

        assertThat(updated).isEqualTo(0);
    }

    @Test
    void doesNotReclaimSucceededSnapshot() {
        User user = userRepository.save(User.create());
        LocalDate date = LocalDate.of(2026, 8, 6);
        DailyContextSnapshot snapshot = DailyContextSnapshot.start(
                user, date, DiaryContextType.WEATHER);
        snapshot.succeed(null);
        snapshotRepository.saveAndFlush(snapshot);
        backdateUpdatedAt(snapshot.getId(), LocalDateTime.now().minusMinutes(10));

        LocalDateTime now = LocalDateTime.now();
        int updated = snapshotRepository.reclaimStale(
                user.getId(),
                date,
                DiaryContextType.WEATHER,
                DailyContextCollectionStatus.IN_PROGRESS,
                now,
                now.minusMinutes(5)
        );

        assertThat(updated).isEqualTo(0);
    }

    private void backdateUpdatedAt(Long snapshotId, LocalDateTime updatedAt) {
        entityManager.getEntityManager()
                .createQuery(
                        "UPDATE DailyContextSnapshot s "
                                + "SET s.updatedAt = :updatedAt WHERE s.id = :id")
                .setParameter("updatedAt", updatedAt)
                .setParameter("id", snapshotId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }
}
