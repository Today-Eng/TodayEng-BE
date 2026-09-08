package com.example.todayEng.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("prod")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FlywayExistingSchemaBootIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Environment environment;

    @DynamicPropertySource
    static void existingDatasource(DynamicPropertyRegistry registry) {
        MYSQL.start();
        initializeExistingSchema();

        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("FLYWAY_BASELINE_ON_MIGRATE", () -> "true");
    }

    @Test
    @DisplayName("운영 설정은 기존 스키마를 version 1 baseline으로 등록한다")
    void baselinesExistingSchemaThroughProductionConfiguration() {
        assertThat(environment.getProperty(
                "spring.flyway.baseline-on-migrate",
                Boolean.class
        )).isTrue();

        Map<String, Object> history = jdbcTemplate.queryForMap(
                "select version, type, success "
                        + "from flyway_schema_history where version = '1'"
        );

        assertThat(history.get("version")).hasToString("1");
        assertThat(history.get("type")).isEqualTo("BASELINE");
        assertThat(history.get("success")).isEqualTo(true);
    }

    private static void initializeExistingSchema() {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ); Statement statement = connection.createStatement()) {
            try {
                statement.execute("set foreign_key_checks = 0");
                ScriptUtils.executeSqlScript(
                        connection,
                        new ClassPathResource("db/migration/V1__baseline.sql")
                );
            } finally {
                statement.execute("set foreign_key_checks = 1");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize existing schema", exception);
        }
    }
}
