package com.example.todayEng.domain.diary.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextCollectionStatus;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class DiaryRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired DiaryRepository diaryRepository;

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
}
