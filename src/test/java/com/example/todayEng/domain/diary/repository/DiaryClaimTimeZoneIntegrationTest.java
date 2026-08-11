package com.example.todayEng.domain.diary.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextCollectionStatus;
import com.example.todayEng.domain.diary.entity.enums.ReflectionQuestionGenerationStatus;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Claim 시각이 JDBC 계층에서 시간대 보정을 한 번 더 받지 않는지 실제 MySQL로 고정한다.
 * H2는 Connector/J의 변환을 재현하지 않아 이 경계를 잡지 못한다.
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DiaryClaimTimeZoneIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-10T04:21:00Z");
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime SERVICE_NOW =
            LocalDateTime.now(Clock.fixed(NOW, SERVICE_ZONE));
    private static final DateTimeFormatter COLUMN_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static TimeZone originalTimeZone;

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withEnv("TZ", "UTC")
            .withCommand("--default-time-zone=+00:00");

    @Autowired UserRepository userRepository;
    @Autowired DiaryRepository diaryRepository;
    @Autowired EntityManager entityManager;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    /** 운영 컨테이너처럼 JVM 기본 시간대가 서비스 시간대와 다른 상황을 재현한다. */
    @BeforeAll
    static void useNonServiceJvmTimeZone() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    static void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    @DisplayName("컨텍스트 수집 Claim은 서비스 시간대 값 그대로 저장된다")
    void storesContextCollectionClaimWithoutTimeZoneShift() {
        Diary diary = savedDiary();

        diaryRepository.claimContextCollection(
                diary.getId(), diary.getUser().getId(), SERVICE_NOW);

        assertThat(reload(diary).getContextCollectionClaimedAt()).isEqualTo(SERVICE_NOW);
        assertThat(storedColumn(diary, "context_collection_claimed_at"))
                .isEqualTo(SERVICE_NOW.format(COLUMN_FORMAT));
    }

    @Test
    @DisplayName("질문 생성 Claim도 서비스 시간대 값 그대로 저장된다")
    void storesQuestionGenerationClaimWithoutTimeZoneShift() {
        Diary diary = savedDiary();

        diaryRepository.claimQuestionGeneration(
                diary.getId(), diary.getUser().getId(), SERVICE_NOW);

        assertThat(reload(diary).getQuestionGenerationClaimedAt()).isEqualTo(SERVICE_NOW);
        assertThat(storedColumn(diary, "question_generation_claimed_at"))
                .isEqualTo(SERVICE_NOW.format(COLUMN_FORMAT));
    }

    @Test
    @DisplayName("컨텍스트 수집 stale 판정은 저장된 절대값이 아니라 경과 시간을 따른다")
    void reclaimsStaleContextCollectionByElapsedTime() {
        Diary diary = savedDiary();
        diaryRepository.claimContextCollection(
                diary.getId(), diary.getUser().getId(), SERVICE_NOW.minusMinutes(11));

        int reclaimed = diaryRepository.reclaimStaleContextCollection(
                diary.getId(), diary.getUser().getId(),
                SERVICE_NOW, SERVICE_NOW.minusMinutes(10));

        assertThat(reclaimed).isEqualTo(1);
        Diary reloaded = reload(diary);
        assertThat(reloaded.getContextCollectionStatus())
                .isEqualTo(DiaryContextCollectionStatus.COLLECTING);
        assertThat(reloaded.getContextCollectionClaimedAt()).isEqualTo(SERVICE_NOW);
        assertThat(reloaded.getContextCollectionLeaseVersion()).isEqualTo(2L);
    }

    @Test
    @DisplayName("질문 생성 stale 판정도 경과 시간을 따른다")
    void reclaimsStaleQuestionGenerationByElapsedTime() {
        Diary diary = savedDiary();
        diaryRepository.claimQuestionGeneration(
                diary.getId(), diary.getUser().getId(), SERVICE_NOW.minusMinutes(11));

        int reclaimed = diaryRepository.reclaimStaleQuestionGeneration(
                diary.getId(), diary.getUser().getId(),
                SERVICE_NOW, SERVICE_NOW.minusMinutes(10));

        assertThat(reclaimed).isEqualTo(1);
        Diary reloaded = reload(diary);
        assertThat(reloaded.getQuestionGenerationStatus())
                .isEqualTo(ReflectionQuestionGenerationStatus.GENERATING);
        assertThat(reloaded.getQuestionGenerationClaimedAt()).isEqualTo(SERVICE_NOW);
        assertThat(reloaded.getQuestionGenerationLeaseVersion()).isEqualTo(2L);
    }

    @Test
    @DisplayName("아직 유효한 Claim은 두 경로 모두 재획득되지 않는다")
    void doesNotReclaimRecentClaims() {
        Diary diary = savedDiary();
        diaryRepository.claimContextCollection(
                diary.getId(), diary.getUser().getId(), SERVICE_NOW.minusMinutes(1));
        diaryRepository.claimQuestionGeneration(
                diary.getId(), diary.getUser().getId(), SERVICE_NOW.minusMinutes(1));

        int reclaimedContext = diaryRepository.reclaimStaleContextCollection(
                diary.getId(), diary.getUser().getId(),
                SERVICE_NOW, SERVICE_NOW.minusMinutes(10));
        int reclaimedQuestion = diaryRepository.reclaimStaleQuestionGeneration(
                diary.getId(), diary.getUser().getId(),
                SERVICE_NOW, SERVICE_NOW.minusMinutes(10));

        assertThat(reclaimedContext).isZero();
        assertThat(reclaimedQuestion).isZero();
    }

    private Diary savedDiary() {
        User user = userRepository.save(User.create());
        return diaryRepository.saveAndFlush(
                Diary.create(user, LocalDate.of(2026, 8, 10)));
    }

    private Diary reload(Diary diary) {
        entityManager.clear();
        return diaryRepository.findById(diary.getId()).orElseThrow();
    }

    /** 엔티티 매핑을 거치지 않고 컬럼에 실제로 들어간 문자열을 확인한다. */
    private String storedColumn(Diary diary, String column) {
        return entityManager.createNativeQuery(
                        "select date_format(" + column + ", '%Y-%m-%d %H:%i:%s') "
                                + "from diary where diary_id = :id")
                .setParameter("id", diary.getId())
                .getSingleResult()
                .toString();
    }
}
