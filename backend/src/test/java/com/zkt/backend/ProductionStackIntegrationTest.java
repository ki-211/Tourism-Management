package com.zkt.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("prod")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class ProductionStackIntegrationTest {
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("tourism").withUsername("tourism").withPassword("tourism-test-password");
    @Container
    static final GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2025-07-23T15-54-02Z"))
            .withEnv("MINIO_ROOT_USER", "tourism-test").withEnv("MINIO_ROOT_PASSWORD", "tourism-test-secret")
            .withCommand("server", "/data").withExposedPorts(9000);
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.2-alpine"))
            .withCommand("redis-server", "--requirepass", "tourism-redis-test").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "tourism-redis-test");
        registry.add("app.jwt.secret", () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        registry.add("app.cors.allowed-origins", () -> "https://localhost");
        registry.add("app.storage.type", () -> "s3");
        registry.add("app.storage.s3-endpoint", () -> "http://" + minio.getHost() + ':' + minio.getMappedPort(9000));
        registry.add("app.storage.s3-bucket", () -> "tourism-test");
        registry.add("app.storage.s3-access-key", () -> "tourism-test");
        registry.add("app.storage.s3-secret-key", () -> "tourism-test-secret");
        registry.add("app.storage.s3-path-style", () -> true);
        registry.add("app.storage.public-base-url", () -> "https://localhost");
        registry.add("app.amap-web-key", () -> "test-amap-key");
    }

    @Test
    void productionDependenciesAreReady() {
        assertThat(mysql.isRunning()).isTrue();
        assertThat(minio.isRunning()).isTrue();
        assertThat(redis.isRunning()).isTrue();
    }
}
