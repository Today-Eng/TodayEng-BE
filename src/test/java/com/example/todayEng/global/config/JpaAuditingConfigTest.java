package com.example.todayEng.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.DateTimeProvider;

class JpaAuditingConfigTest {

    private static final Instant NOW = Instant.parse("2026-08-10T04:21:00Z");

    private final TimeZone originalTimeZone = TimeZone.getDefault();

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    @DisplayName("JVM 기본 시간대가 무엇이든 감사 시각은 서비스 Clock을 따른다")
    void usesServiceClockRegardlessOfJvmTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Clock serviceClock = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"));

        DateTimeProvider provider =
                new JpaAuditingConfig().auditingDateTimeProvider(serviceClock);

        assertThat(provider.getNow()).contains(LocalDateTime.of(2026, 8, 10, 13, 21));
    }

    @Test
    @DisplayName("감사 시각과 Clock으로 기록하는 claimedAt이 같은 기준을 쓴다")
    void matchesClockBasedTimestamps() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        Clock serviceClock = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"));

        DateTimeProvider provider =
                new JpaAuditingConfig().auditingDateTimeProvider(serviceClock);

        assertThat(provider.getNow()).contains(LocalDateTime.now(serviceClock));
    }
}
