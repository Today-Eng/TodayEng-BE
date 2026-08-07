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
        assertThat(reclaimed.getLeaseVersion()).isEqualTo(1L);
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

    @Test
    void finishIfOwnedTransitionsMatchingInProgressSnapshotToSucceeded() {
        User user = userRepository.save(User.create());
        LocalDate date = LocalDate.of(2026, 8, 6);
        DailyContextSnapshot snapshot = snapshotRepository.saveAndFlush(
                DailyContextSnapshot.start(user, date, DiaryContextType.WEATHER));

        int updated = snapshotRepository.finishIfOwned(
                snapshot.getId(),
                DailyContextCollectionStatus.SUCCEEDED,
                DailyContextCollectionStatus.IN_PROGRESS,
                0L
        );

        assertThat(updated).isEqualTo(1);
        DailyContextSnapshot finished = snapshotRepository.findById(snapshot.getId())
                .orElseThrow();
        assertThat(finished.getCollectionStatus())
                .isEqualTo(DailyContextCollectionStatus.SUCCEEDED);
        assertThat(finished.getLeaseVersion()).isEqualTo(1L);
    }

    @Test
    void finishIfOwnedRejectsMismatchedLeaseVersion() {
        User user = userRepository.save(User.create());
        LocalDate date = LocalDate.of(2026, 8, 6);
        DailyContextSnapshot snapshot = snapshotRepository.saveAndFlush(
                DailyContextSnapshot.start(user, date, DiaryContextType.WEATHER));

        int updated = snapshotRepository.finishIfOwned(
                snapshot.getId(),
                DailyContextCollectionStatus.SUCCEEDED,
                DailyContextCollectionStatus.IN_PROGRESS,
                99L
        );

        assertThat(updated).isEqualTo(0);
        DailyContextSnapshot untouched = snapshotRepository.findById(snapshot.getId())
                .orElseThrow();
        assertThat(untouched.getCollectionStatus())
                .isEqualTo(DailyContextCollectionStatus.IN_PROGRESS);
    }

    @Test
    void overlappingCollectorsOnlyReclaimerWinsFinalWrite() {
        User user = userRepository.save(User.create());
        LocalDate date = LocalDate.of(2026, 8, 6);
        DailyContextSnapshot snapshot = snapshotRepository.saveAndFlush(
                DailyContextSnapshot.start(user, date, DiaryContextType.WEATHER));
        long originalLeaseVersion = snapshot.getLeaseVersion();

        // The first collector's snapshot goes stale (e.g. it hangs past the timeout)
        // and a second request reclaims the same row.
        backdateUpdatedAt(snapshot.getId(), LocalDateTime.now().minusMinutes(10));
        LocalDateTime now = LocalDateTime.now();
        int reclaimed = snapshotRepository.reclaimStale(
                user.getId(),
                date,
                DiaryContextType.WEATHER,
                DailyContextCollectionStatus.IN_PROGRESS,
                now,
                now.minusMinutes(5)
        );
        assertThat(reclaimed).isEqualTo(1);
        long reclaimerLeaseVersion = snapshotRepository
                .findByUser_IdAndContextDateAndContextType(
                        user.getId(), date, DiaryContextType.WEATHER)
                .orElseThrow()
                .getLeaseVersion();

        // The original (now-stale) collector finally finishes and tries to write
        // using the lease version it captured before the reclaim happened.
        int staleWrite = snapshotRepository.finishIfOwned(
                snapshot.getId(),
                DailyContextCollectionStatus.SUCCEEDED,
                DailyContextCollectionStatus.IN_PROGRESS,
                originalLeaseVersion
        );
        assertThat(staleWrite).isEqualTo(0);
        assertThat(snapshotRepository.findById(snapshot.getId()).orElseThrow()
                .getCollectionStatus()).isEqualTo(DailyContextCollectionStatus.IN_PROGRESS);

        // The reclaimer finishes and writes using its own lease version.
        int winningWrite = snapshotRepository.finishIfOwned(
                snapshot.getId(),
                DailyContextCollectionStatus.SUCCEEDED,
                DailyContextCollectionStatus.IN_PROGRESS,
                reclaimerLeaseVersion
        );
        assertThat(winningWrite).isEqualTo(1);
        assertThat(snapshotRepository.findById(snapshot.getId()).orElseThrow()
                .getCollectionStatus()).isEqualTo(DailyContextCollectionStatus.SUCCEEDED);
    }

    @Test
    void deletesOnlyTheSnapshotMatchingUserDateAndType() {
        User user = userRepository.save(User.create());
        LocalDate date = LocalDate.of(2026, 8, 6);
        DailyContextSnapshot weather = snapshotRepository.saveAndFlush(
                DailyContextSnapshot.start(user, date, DiaryContextType.WEATHER));
        DailyContextSnapshot calendar = snapshotRepository.saveAndFlush(
                DailyContextSnapshot.start(user, date, DiaryContextType.CALENDAR));

        long deleted = snapshotRepository.deleteAllByUserIdAndContextDateAndContextType(
                user.getId(), date, DiaryContextType.WEATHER);

        assertThat(deleted).isEqualTo(1);
        assertThat(snapshotRepository.findById(weather.getId())).isEmpty();
        assertThat(snapshotRepository.findById(calendar.getId())).isPresent();
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
