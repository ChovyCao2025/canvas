package org.chovy.canvas.testsupport;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.Map;

@Testcontainers
public abstract class CanvasIntegrationTestBase {

    @Container
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("canvas_it")
            .withUsername("canvas")
            .withPassword("canvas");

    @Container
    protected static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static volatile boolean migrated;

    @org.junit.jupiter.api.BeforeAll
    static void migrateSchema() {
        if (migrated) {
            return;
        }
        synchronized (CanvasIntegrationTestBase.class) {
            if (migrated) {
                return;
            }
            Flyway.configure()
                    .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .placeholderReplacement(false)
                    .load()
                    .migrate();
            migrated = true;
        }
    }

    protected static JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

    protected static DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(MYSQL.getDriverClassName());
        dataSource.setUrl(MYSQL.getJdbcUrl());
        dataSource.setUsername(MYSQL.getUsername());
        dataSource.setPassword(MYSQL.getPassword());
        return dataSource;
    }

    protected static Map<String, String> springProperties() {
        return Map.of(
                "spring.datasource.url", MYSQL.getJdbcUrl(),
                "spring.datasource.username", MYSQL.getUsername(),
                "spring.datasource.password", MYSQL.getPassword(),
                "spring.data.redis.host", REDIS.getHost(),
                "spring.data.redis.port", String.valueOf(REDIS.getMappedPort(6379)),
                "spring.flyway.enabled", "true",
                "spring.flyway.placeholder-replacement", "false"
        );
    }

    @org.springframework.test.context.DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.flyway.placeholder-replacement", () -> false);
    }
}
