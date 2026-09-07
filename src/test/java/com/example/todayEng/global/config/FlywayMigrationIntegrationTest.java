package com.example.todayEng.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
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

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> EXISTING_MYSQL = new MySQLContainer<>("mysql:8.4");

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

        assertThat(tables)
                .contains("flyway_schema_history", "users", "diary", "shedlock");
        assertThat(jdbcTemplate.queryForObject(
                "select success from flyway_schema_history where version = '1'",
                Boolean.class
        )).isTrue();
        assertThat(entityManager.getMetamodel().getEntities()).isNotEmpty();
    }

    @Test
    @DisplayName("기존 운영 스키마는 V1 SQL을 재실행하지 않고 baseline 이력으로 등록한다")
    void baselinesExistingDatabaseAtVersionOne() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                EXISTING_MYSQL.getJdbcUrl(),
                EXISTING_MYSQL.getUsername(),
                EXISTING_MYSQL.getPassword()
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
        }

        Flyway.configure()
                .dataSource(
                        EXISTING_MYSQL.getJdbcUrl(),
                        EXISTING_MYSQL.getUsername(),
                        EXISTING_MYSQL.getPassword()
                )
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                EXISTING_MYSQL.getJdbcUrl(),
                EXISTING_MYSQL.getUsername(),
                EXISTING_MYSQL.getPassword()
        ); PreparedStatement statement = connection.prepareStatement(
                "select version, type, success from flyway_schema_history where version = '1'"
        ); ResultSet result = statement.executeQuery()) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString("version")).isEqualTo("1");
            assertThat(result.getString("type")).isEqualTo("BASELINE");
            assertThat(result.getBoolean("success")).isTrue();
            assertThat(result.next()).isFalse();
        }
    }
}
