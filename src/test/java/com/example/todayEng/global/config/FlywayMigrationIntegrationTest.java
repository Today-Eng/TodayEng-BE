package com.example.todayEng.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FlywayMigrationIntegrationTest {

    private static final int TABLE_COUNT_INCLUDING_FLYWAY_HISTORY = 19;

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    @DisplayName("빈 MySQL DB에 baseline을 적용하고 JPA mapping을 검증한다")
    void migratesEmptyDatabaseAndValidatesJpaMappings() {
        List<String> tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables "
                        + "where table_schema = database()",
                String.class
        );

        assertThat(tables).hasSize(TABLE_COUNT_INCLUDING_FLYWAY_HISTORY);
        assertThat(tables)
                .contains("flyway_schema_history", "users", "diary", "shedlock");
        assertThat(jdbcTemplate.queryForObject(
                "select success from flyway_schema_history where version = '1'",
                Boolean.class
        )).isTrue();
        assertThat(entityManager.getMetamodel().getEntities()).isNotEmpty();
    }
}
