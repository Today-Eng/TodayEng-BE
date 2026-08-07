package com.example.todayEng.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;

import java.sql.Connection;
import java.sql.SQLException;

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

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            Integer acquired = queryLockResult(
                    connection,
                    "SELECT GET_LOCK('todayeng_reference_data_seed', 30)"
            );
            if (!Integer.valueOf(1).equals(acquired)) {
                throw new IllegalStateException("기준 데이터 시딩 잠금을 획득하지 못했습니다.");
            }
            try {
                seedReferenceData();
            } finally {
                queryLockResult(
                        connection,
                        "SELECT RELEASE_LOCK('todayeng_reference_data_seed')"
                );
            }
            return null;
        });
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

    private Integer queryLockResult(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getObject(1, Integer.class) : null;
        }
    }
}
