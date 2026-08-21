package com.example.youtubebot.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public abstract class PostgreSqlIntegrationTest {

    private static final String TOKEN_ENCRYPTION_KEY = generateEncryptionKey();

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse(
                            "postgres:17.11-alpine3.24@sha256:18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73")
                    .asCompatibleSubstituteFor("postgres"));

    static {
        POSTGRES.start();
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    protected static void integrationProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("youtube.token-encryption.key", () -> TOKEN_ENCRYPTION_KEY);
        registry.add("youtube.token-encryption.key-version", () -> 3);
    }

    @BeforeEach
    protected void clearDatabase() {
        jdbcTemplate.execute(
                "TRUNCATE video_comment_guard, comment_attempt, ai_generation, "
                        + "video_context, oauth_connection CASCADE");
    }

    private static String generateEncryptionKey() {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        String encoded = Base64.getEncoder().encodeToString(keyBytes);
        Arrays.fill(keyBytes, (byte) 0);
        return encoded;
    }
}
