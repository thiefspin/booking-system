package com.thiefspin.bookingsystem;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that require a real PostgreSQL database.
 * This class uses Testcontainers to spin up a PostgreSQL container for testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("booking_system_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true)
            .withCommand("postgres", "-c", "max_connections=200");

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "0");
        registry.add("spring.flyway.schemas", () -> "booking");
        registry.add("spring.flyway.create-schemas", () -> "true");
        registry.add("spring.flyway.clean-on-validation-error", () -> "true");
        registry.add("spring.flyway.validate-on-migrate", () -> "false");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.sql.init.schema-locations", () -> "");
        registry.add("spring.sql.init.data-locations", () -> "");

        registry.add("logging.level.org.flywaydb", () -> "DEBUG");
        registry.add("logging.level.org.springframework.jdbc", () -> "DEBUG");
    }

    @BeforeAll
    static void beforeAll() {
        POSTGRES_CONTAINER.start();
    }
}
