package com.example.todayEng.domain.diary.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextCollectionStatus;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class DiaryRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired DiaryRepository diaryRepository;

    @Test
    void findsOnlyCompletedDiaryStatisticsInDescendingDateOrder() {
        User user = userRepository.save(User.create());
        Diary completedToday = Diary.create(user, LocalDate.of(2026, 8, 10));
        completedToday.complete();
        Diary completedYesterday = Diary.create(user, LocalDate.of(2026, 8, 9));
        completedYesterday.complete();
        Diary deleted = Diary.create(user, LocalDate.of(2026, 8, 8));
        deleted.complete();
        deleted.delete();
        Diary inProgress = Diary.create(user, LocalDate.of(2026, 8, 7));
        diaryRepository.saveAllAndFlush(List.of(
                completedYesterday, deleted, inProgress, completedToday
        ));

        long count = diaryRepository.countByUserIdAndStatus(
                user.getId(), DiaryStatus.COMPLETED);
        var dates = diaryRepository.findCompletedDatesForStreak(
                user.getId(), LocalDate.of(2026, 8, 10),
                PageRequest.of(0, 32));

        assertThat(count).isEqualTo(2);
        assertThat(dates).containsExactly(
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 9)
        );
    }

    @Test
    void claimsContextCollectionFromNotStartedAndStampsClaimedAt() {
        User user = userRepository.save(User.create());
        Diary diary = diaryRepository.saveAndFlush(
                Diary.create(user, LocalDate.of(2026, 8, 7)));
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        int claimed = diaryRepository.claimContextCollection(diary.getId(), user.getId(), now);

        assertThat(claimed).isEqualTo(1);
        Diary reloaded = diaryRepository.findById(diary.getId()).orElseThrow();
        assertThat(reloaded.getContextCollectionStatus())
                .isEqualTo(DiaryContextCollectionStatus.COLLECTING);
        assertThat(reloaded.getContextCollectionClaimedAt()).isEqualTo(now);
        assertThat(reloaded.getContextCollectionLeaseVersion()).isEqualTo(1L);
    }

    @Test
    void doesNotClaimAlreadyCollectingContextCollection() {
        User user = userRepository.save(User.create());
        Diary diary = diaryRepository.saveAndFlush(
                Diary.create(user, LocalDate.of(2026, 8, 7)));
        LocalDateTime firstClaim = LocalDateTime.now();
        diaryRepository.claimContextCollection(diary.getId(), user.getId(), firstClaim);

        int secondClaim = diaryRepository.claimContextCollection(
                diary.getId(), user.getId(), LocalDateTime.now());

        assertThat(secondClaim).isEqualTo(0);
    }

    @Test
    void reclaimsStaleCollectingContextCollectionAfterTimeout() {
        User user = userRepository.save(User.create());
        Diary diary = diaryRepository.saveAndFlush(
                Diary.create(user, LocalDate.of(2026, 8, 7)));
        LocalDateTime staleClaim = LocalDateTime.now().minusMinutes(20);
        diaryRepository.claimContextCollection(diary.getId(), user.getId(), staleClaim);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        int reclaimed = diaryRepository.reclaimStaleContextCollection(
                diary.getId(), user.getId(), now, now.minusMinutes(10));

        assertThat(reclaimed).isEqualTo(1);
        Diary reloaded = diaryRepository.findById(diary.getId()).orElseThrow();
        assertThat(reloaded.getContextCollectionStatus())
                .isEqualTo(DiaryContextCollectionStatus.COLLECTING);
        assertThat(reloaded.getContextCollectionClaimedAt()).isEqualTo(now);
        assertThat(reloaded.getContextCollectionLeaseVersion()).isEqualTo(2L);
    }

    @Test
    void doesNotReclaimRecentlyClaimedContextCollection() {
        User user = userRepository.save(User.create());
        Diary diary = diaryRepository.saveAndFlush(
                Diary.create(user, LocalDate.of(2026, 8, 7)));
        LocalDateTime recentClaim = LocalDateTime.now().minusMinutes(1);
        diaryRepository.claimContextCollection(diary.getId(), user.getId(), recentClaim);

        LocalDateTime now = LocalDateTime.now();
        int reclaimed = diaryRepository.reclaimStaleContextCollection(
                diary.getId(), user.getId(), now, now.minusMinutes(10));

        assertThat(reclaimed).isEqualTo(0);
    }

    @Test
    void finishContextCollectionIfOwnedTransitionsMatchingLease() {
        User user = userRepository.save(User.create());
        Diary diary = diaryRepository.saveAndFlush(
                Diary.create(user, LocalDate.of(2026, 8, 7)));
        diaryRepository.claimContextCollection(diary.getId(), user.getId(), LocalDateTime.now());

        int finished = diaryRepository.finishContextCollectionIfOwned(
                diary.getId(), user.getId(), DiaryContextCollectionStatus.COMPLETED, 1L);

        assertThat(finished).isEqualTo(1);
        assertThat(diaryRepository.findById(diary.getId()).orElseThrow()
                .getContextCollectionStatus()).isEqualTo(DiaryContextCollectionStatus.COMPLETED);
    }

    @Test
    void finishContextCollectionIfOwnedRejectsMismatchedLease() {
        User user = userRepository.save(User.create());
        Diary diary = diaryRepository.saveAndFlush(
                Diary.create(user, LocalDate.of(2026, 8, 7)));
        diaryRepository.claimContextCollection(diary.getId(), user.getId(), LocalDateTime.now());

        int finished = diaryRepository.finishContextCollectionIfOwned(
                diary.getId(), user.getId(), DiaryContextCollectionStatus.COMPLETED, 99L);

        assertThat(finished).isEqualTo(0);
        assertThat(diaryRepository.findById(diary.getId()).orElseThrow()
                .getContextCollectionStatus()).isEqualTo(DiaryContextCollectionStatus.COLLECTING);
    }

    @Test
    void verifyContextCollectionLeaseRejectsStaleLeaseAfterReclaim() {
        User user = userRepository.save(User.create());
        Diary diary = diaryRepository.saveAndFlush(
                Diary.create(user, LocalDate.of(2026, 8, 7)));
        diaryRepository.claimContextCollection(
                diary.getId(), user.getId(), LocalDateTime.now().minusMinutes(20));
        long originalLeaseVersion = diaryRepository.findById(diary.getId())
                .orElseThrow().getContextCollectionLeaseVersion();

        LocalDateTime now = LocalDateTime.now();
        int reclaimed = diaryRepository.reclaimStaleContextCollection(
                diary.getId(), user.getId(), now, now.minusMinutes(10));
        assertThat(reclaimed).isEqualTo(1);

        int staleVerify = diaryRepository.verifyContextCollectionLease(
                diary.getId(), user.getId(), LocalDateTime.now(), originalLeaseVersion);
        assertThat(staleVerify).isEqualTo(0);

        long reclaimerLeaseVersion = diaryRepository.findById(diary.getId())
                .orElseThrow().getContextCollectionLeaseVersion();
        int freshVerify = diaryRepository.verifyContextCollectionLease(
                diary.getId(), user.getId(), LocalDateTime.now(), reclaimerLeaseVersion);
        assertThat(freshVerify).isEqualTo(1);
    }

    @Test
    void overlappingClaimantsOnlyReclaimerWinsFinalCompletion() {
        User user = userRepository.save(User.create());
        Diary diary = diaryRepository.saveAndFlush(
                Diary.create(user, LocalDate.of(2026, 8, 7)));

        // Original claimant claims, then stalls past the timeout.
        diaryRepository.claimContextCollection(
                diary.getId(), user.getId(), LocalDateTime.now().minusMinutes(20));
        long originalLeaseVersion = diaryRepository.findById(diary.getId())
                .orElseThrow().getContextCollectionLeaseVersion();

        // A second request reclaims the same diary.
        LocalDateTime now = LocalDateTime.now();
        int reclaimed = diaryRepository.reclaimStaleContextCollection(
                diary.getId(), user.getId(), now, now.minusMinutes(10));
        assertThat(reclaimed).isEqualTo(1);
        long reclaimerLeaseVersion = diaryRepository.findById(diary.getId())
                .orElseThrow().getContextCollectionLeaseVersion();

        // The original (now-stale) claimant finally finishes using its old lease.
        int staleCompletion = diaryRepository.finishContextCollectionIfOwned(
                diary.getId(), user.getId(),
                DiaryContextCollectionStatus.COMPLETED, originalLeaseVersion);
        assertThat(staleCompletion).isEqualTo(0);
        assertThat(diaryRepository.findById(diary.getId()).orElseThrow()
                .getContextCollectionStatus()).isEqualTo(DiaryContextCollectionStatus.COLLECTING);

        // The reclaimer finishes using its own lease.
        int winningCompletion = diaryRepository.finishContextCollectionIfOwned(
                diary.getId(), user.getId(),
                DiaryContextCollectionStatus.COMPLETED, reclaimerLeaseVersion);
        assertThat(winningCompletion).isEqualTo(1);
        assertThat(diaryRepository.findById(diary.getId()).orElseThrow()
                .getContextCollectionStatus()).isEqualTo(DiaryContextCollectionStatus.COMPLETED);
    }
}
