package com.zkt.backend;

import com.zkt.backend.media.ObjectStorage;
import com.zkt.backend.media.S3ObjectStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("prod")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class ProductionStackIntegrationTest {
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("tourism").withUsername("tourism").withPassword("tourism-test-password");
    // minio/minio was removed from Docker Hub; s3mock speaks the same S3 API that S3ObjectStorage uses.
    @Container
    static final GenericContainer<?> s3 = new GenericContainer<>(DockerImageName.parse("adobe/s3mock:5.2.2"))
            .withExposedPorts(9090);
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
        registry.add("app.storage.s3-endpoint", () -> "http://" + s3.getHost() + ':' + s3.getMappedPort(9090));
        registry.add("app.storage.s3-bucket", () -> "tourism-test");
        registry.add("app.storage.s3-access-key", () -> "tourism-test");
        registry.add("app.storage.s3-secret-key", () -> "tourism-test-secret");
        registry.add("app.storage.s3-path-style", () -> true);
        registry.add("app.storage.public-base-url", () -> "https://localhost");
        registry.add("app.amap-web-key", () -> "test-amap-key");
    }

    @Autowired ObjectStorage storage;

    @Test
    void productionDependenciesAreReady() throws Exception {
        assertThat(mysql.isRunning()).isTrue();
        assertThat(s3.isRunning()).isTrue();
        assertThat(redis.isRunning()).isTrue();
        assertThat(storage).isInstanceOf(S3ObjectStorage.class);
        byte[] content = "tourism".getBytes(StandardCharsets.UTF_8);
        storage.put("smoke/object.txt", content, "text/plain");
        try (InputStream in = storage.open("smoke/object.txt")) { assertThat(in.readAllBytes()).isEqualTo(content); }
        storage.delete("smoke/object.txt");
    }
}
