package com.example.todayEng.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;

@Component
@RequiredArgsConstructor
public class ReferenceDataSeeder implements ApplicationRunner {

    private final TermsSeeder termsSeeder;
    private final InterestTagSeeder interestTagSeeder;
    private final TermsSchemaCompatibility termsSchemaCompatibility;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!isMySql()) {
            seedReferenceData();
            return;
        }

        Integer acquired = jdbcTemplate.queryForObject(
                "SELECT GET_LOCK('todayeng_reference_data_seed', 30)", Integer.class);
        if (!Integer.valueOf(1).equals(acquired)) {
            throw new IllegalStateException("기준 데이터 시딩 잠금을 획득하지 못했습니다.");
        }
        try {
            seedReferenceData();
        } finally {
            jdbcTemplate.queryForObject(
                    "SELECT RELEASE_LOCK('todayeng_reference_data_seed')", Integer.class);
        }
    }

    private void seedReferenceData() {
        termsSchemaCompatibility.prepare();
        termsSeeder.seed();
        interestTagSeeder.seed();
    }

    private boolean isMySql() {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection ->
                connection.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql")));
    }
}
