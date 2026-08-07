package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextCollectionStatus;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.repository.DiaryContextRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * DiaryContextPersistenceService is injected through {@code @Import} rather than
 * constructed with {@code new}, so its {@code @Transactional} methods run through the real
 * Spring proxy and the verify-then-write sequence inside saveSuccess/saveFailure executes as
 * the single atomic unit it does in production.
 *
 * Note that {@code @DataJpaTest} still wraps the whole test method in one transaction, so all
 * calls here share that transaction and persistence context. What this proves is that the
 * lease CAS conditions (WHERE collectionStatus = COLLECTING AND leaseVersion = ?) correctly
 * accept or reject each write against the row's current state, and that saveSuccess's atomic
 * verify+persist no longer skips the verify step. It does not prove row-lock based
 * serialization between two genuinely concurrent transactions.
 */
@DataJpaTest
@Import({
        DiaryContextPersistenceService.class,
        DiaryContextPersistenceServiceTest.Config.class
})
class DiaryContextPersistenceServiceTest {

    @Autowired UserRepository userRepository;
    @Autowired DiaryRepository diaryRepository;
    @Autowired DiaryContextRepository contextRepository;
    @Autowired DiaryContextPersistenceService service;

    User user;
    Diary diary;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.create());
        diary = diaryRepository.saveAndFlush(Diary.create(user, LocalDate.of(2026, 8, 7)));
    }

    @Test
    void staleClaimantsWriteIsRejectedOnceAnotherClaimantHasReclaimed() throws Exception {
        // Original claimant claims, then stalls past the timeout.
        diaryRepository.claimContextCollection(
                diary.getId(), user.getId(), LocalDateTime.now().minusMinutes(20));
        long originalLeaseVersion = currentLeaseVersion();

        // A second request reclaims the same diary.
        LocalDateTime now = LocalDateTime.now();
        int reclaimed = diaryRepository.reclaimStaleContextCollection(
                diary.getId(), user.getId(), now, now.minusMinutes(10));
        assertThat(reclaimed).isEqualTo(1);
        long reclaimerLeaseVersion = currentLeaseVersion();
        assertThat(reclaimerLeaseVersion).isNotEqualTo(originalLeaseVersion);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode staleData = objectMapper.readTree("{\"temperature\":10}");
        JsonNode freshData = objectMapper.readTree("{\"temperature\":25}");

        // The original (now-stale) claimant finally finishes its slow external call
        // and tries to persist using the lease version it captured before the reclaim.
        Optional<DiaryContext> staleWrite = service.saveSuccess(
                user.getId(), diary.getId(), originalLeaseVersion,
                DiaryContextType.WEATHER, staleData);
        assertThat(staleWrite).isEmpty();
        assertThat(contextRepository.findByDiaryAndContextType(diary, DiaryContextType.WEATHER))
                .isEmpty();

        // The reclaimer finishes and persists using its own lease version.
        Optional<DiaryContext> winningWrite = service.saveSuccess(
                user.getId(), diary.getId(), reclaimerLeaseVersion,
                DiaryContextType.WEATHER, freshData);
        assertThat(winningWrite).isPresent();
        DiaryContext saved = contextRepository
                .findByDiaryAndContextType(diary, DiaryContextType.WEATHER)
                .orElseThrow();
        assertThat(saved.getContextData().path("temperature").asInt()).isEqualTo(25);

        // If the stale claimant's write had gone through unguarded, this final read
        // would see 10 instead of 25.
        Optional<DiaryContext> lateStaleWrite = service.saveSuccess(
                user.getId(), diary.getId(), originalLeaseVersion,
                DiaryContextType.WEATHER, staleData);
        assertThat(lateStaleWrite).isEmpty();
        assertThat(contextRepository.findByDiaryAndContextType(diary, DiaryContextType.WEATHER)
                .orElseThrow().getContextData().path("temperature").asInt()).isEqualTo(25);
    }

    @Test
    void staleClaimantCannotCompleteOrFailAfterReclaim() {
        diaryRepository.claimContextCollection(
                diary.getId(), user.getId(), LocalDateTime.now().minusMinutes(20));
        long originalLeaseVersion = currentLeaseVersion();

        LocalDateTime now = LocalDateTime.now();
        diaryRepository.reclaimStaleContextCollection(
                diary.getId(), user.getId(), now, now.minusMinutes(10));
        long reclaimerLeaseVersion = currentLeaseVersion();

        service.failContextCollection(user.getId(), diary.getId(), originalLeaseVersion);
        assertThat(diaryRepository.findById(diary.getId()).orElseThrow()
                .getContextCollectionStatus()).isEqualTo(DiaryContextCollectionStatus.COLLECTING);

        service.completeContextCollection(user.getId(), diary.getId(), reclaimerLeaseVersion);
        assertThat(diaryRepository.findById(diary.getId()).orElseThrow()
                .getContextCollectionStatus()).isEqualTo(DiaryContextCollectionStatus.COMPLETED);
    }

    private long currentLeaseVersion() {
        return diaryRepository.findById(diary.getId())
                .orElseThrow()
                .getContextCollectionLeaseVersion();
    }

    @TestConfiguration
    static class Config {

        @Bean
        Clock clock() {
            return Clock.system(ZoneId.of("Asia/Seoul"));
        }
    }
}
