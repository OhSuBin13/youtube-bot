package com.example.youtubebot.persistence;

import com.example.youtubebot.oauth.OAuthConnectionCredentials;
import com.example.youtubebot.oauth.OAuthConnectionInput;
import com.example.youtubebot.oauth.OAuthTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PersistenceIT {

    private static final String TOKEN_ENCRYPTION_KEY = generateEncryptionKey();

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void tokenEncryptionProperties(DynamicPropertyRegistry registry) {
        registry.add("youtube.token-encryption.key", () -> TOKEN_ENCRYPTION_KEY);
        registry.add("youtube.token-encryption.key-version", () -> 3);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OAuthTokenStore tokenStore;

    @Autowired
    private VideoContextRepository videoContextRepository;

    @Autowired
    private AiGenerationRepository aiGenerationRepository;

    @Autowired
    private CommentAttemptRepository commentAttemptRepository;

    @Autowired
    private VideoCommentGuardRepository videoCommentGuardRepository;

    @LocalServerPort
    private int serverPort;

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.execute(
                "TRUNCATE video_comment_guard, comment_attempt, ai_generation, "
                        + "video_context, oauth_connection CASCADE");
    }

    @Test
    void actuatorHealthIsPublicAndReportsPostgresqlUp() throws Exception {
        HttpResponse<String> response = get("/actuator/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
        assertTrue(response.body().contains("\"db\""));
        assertTrue(response.body().contains("\"database\":\"PostgreSQL\""));
    }

    @Test
    void protectedPageRedirectsAnonymousUserToLogin() throws Exception {
        HttpResponse<String> response = get("/");

        assertEquals(302, response.statusCode());
        String location = response.headers().firstValue("Location").orElseThrow();
        assertTrue(location.endsWith("/login"));
        assertEquals(200, get("/login").statusCode());
    }

    @Test
    void flywayCreatesExpectedPostgresqlTypesAndConstraints() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'oauth_connection',
                    'video_context',
                    'ai_generation',
                    'comment_attempt',
                    'video_comment_guard'
                  )
                ORDER BY table_name
                """, String.class);

        assertEquals(List.of(
                "ai_generation",
                "comment_attempt",
                "oauth_connection",
                "video_comment_guard",
                "video_context"), tables);
        assertColumnType("oauth_connection", "refresh_token_ciphertext", "bytea");
        assertColumnType("oauth_connection", "refresh_token_nonce", "bytea");
        assertColumnType("video_context", "video_metadata", "jsonb");
        assertColumnType("video_context", "collected_at", "timestamp with time zone");
        assertColumnType("ai_generation", "draft_id", "uuid");
    }

    @Test
    void repositoriesPersistValidJsonAndDomainFormats() {
        Instant now = Instant.parse("2026-08-20T07:00:00Z");
        String videoId = "dQw4w9WgXcQ";
        UUID draftId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        videoContextRepository.saveAndFlush(new VideoContext(
                videoId,
                "https://www.youtube.com/watch?v=" + videoId,
                "{\"title\":\"테스트 영상\"}",
                "{\"title\":\"테스트 채널\"}",
                "[{\"text\":\"좋은 영상입니다\"}]",
                "사용자 요약",
                now,
                now.plus(30, ChronoUnit.DAYS)));
        aiGenerationRepository.saveAndFlush(new AiGeneration(
                draftId,
                videoId,
                "qwen3:4b",
                "v1",
                "유익한 영상 감사합니다.",
                "[\"video.title\"]",
                "sufficient",
                "passed",
                "[]",
                "영상 제목을 근거로 작성",
                "{\"duplicate\":false}",
                "유익한 설명 감사합니다.",
                now,
                now));
        commentAttemptRepository.saveAndFlush(new CommentAttempt(
                attemptId,
                videoId,
                draftId,
                "유익한 영상 감사합니다.",
                "유익한 설명 감사합니다.",
                "UC_AUTHOR_CHANNEL",
                "UC_TARGET_CHANNEL",
                "PUBLISHING",
                null,
                null,
                now,
                now,
                null));
        videoCommentGuardRepository.saveAndFlush(new VideoCommentGuard(
                videoId, "PUBLISHING", attemptId, now, now));

        assertTrue(videoContextRepository.existsById(videoId));
        assertTrue(aiGenerationRepository.existsById(draftId));
        assertTrue(commentAttemptRepository.existsById(attemptId));
        assertTrue(videoCommentGuardRepository.existsById(videoId));
        assertEquals("object", jdbcTemplate.queryForObject(
                "SELECT jsonb_typeof(video_metadata) FROM video_context WHERE video_id = ?",
                String.class,
                videoId));
        assertEquals("array", jdbcTemplate.queryForObject(
                "SELECT jsonb_typeof(evidence_fields) FROM ai_generation WHERE draft_id = ?",
                String.class,
                draftId));
    }

    @Test
    void tokenStorePersistsCiphertextNonceVersionAndRoundTrips() {
        String refreshToken = "1//database-round-trip-token-한글";
        Instant connectedAt = Instant.parse("2026-08-20T07:00:00Z");
        OAuthConnectionInput input = new OAuthConnectionInput(
                refreshToken,
                Set.of("scope-b", "scope-a"),
                "UC_AUTHOR_CHANNEL",
                "작성 채널",
                connectedAt);

        tokenStore.save(input);

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT refresh_token_ciphertext, refresh_token_nonce, key_version, granted_scope
                FROM oauth_connection
                WHERE id = 1
                """);
        byte[] ciphertext = (byte[]) row.get("refresh_token_ciphertext");
        byte[] firstNonce = (byte[]) row.get("refresh_token_nonce");
        assertFalse(Arrays.equals(
                refreshToken.getBytes(StandardCharsets.UTF_8), ciphertext));
        assertEquals(refreshToken.getBytes(StandardCharsets.UTF_8).length + 16, ciphertext.length);
        assertEquals(12, firstNonce.length);
        assertEquals(3, row.get("key_version"));
        assertEquals("scope-a scope-b", row.get("granted_scope"));

        OAuthConnectionCredentials restored = tokenStore.find().orElseThrow();
        assertEquals(refreshToken, restored.refreshToken());
        assertEquals(Set.of("scope-a", "scope-b"), restored.grantedScopes());
        assertEquals("UC_AUTHOR_CHANNEL", restored.channelId());
        assertEquals("작성 채널", restored.channelName());
        assertEquals(connectedAt, restored.connectedAt());

        tokenStore.save(new OAuthConnectionInput(
                refreshToken,
                Set.of("scope-a"),
                "UC_AUTHOR_CHANNEL",
                "작성 채널",
                connectedAt));
        byte[] secondNonce = jdbcTemplate.queryForObject(
                "SELECT refresh_token_nonce FROM oauth_connection WHERE id = 1",
                byte[].class);
        assertNotNull(secondNonce);
        assertNotSame(firstNonce, secondNonce);
        assertFalse(Arrays.equals(firstNonce, secondNonce));
    }

    @Test
    void databaseRejectsInvalidBinaryAndVideoFormats() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO oauth_connection (
                    id, refresh_token_ciphertext, refresh_token_nonce, key_version,
                    granted_scope, channel_id, channel_name, connected_at
                ) VALUES (1, ?, ?, 1, 'scope', 'channel', 'name', now())
                """, new byte[17], new byte[11]));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO video_context (
                    video_id, canonical_url, video_metadata, channel_context,
                    public_comments, collected_at, expires_at
                ) VALUES (
                    'too-short',
                    'https://www.youtube.com/watch?v=too-short',
                    '{}'::jsonb,
                    '{}'::jsonb,
                    '[]'::jsonb,
                    now(),
                    now() + interval '1 day'
                )
                """));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO video_context (
                    video_id, canonical_url, video_metadata, channel_context,
                    public_comments, collected_at, expires_at
                ) VALUES (
                    'dQw4w9WgXcQ',
                    'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
                    '{}'::jsonb,
                    '{}'::jsonb,
                    '[]'::jsonb,
                    now(),
                    now() + interval '31 days'
                )
                """));
    }

    private void assertColumnType(String table, String column, String expectedType) {
        String actualType = jdbcTemplate.queryForObject("""
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """, String.class, table, column);
        assertEquals(expectedType, actualType);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + serverPort + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String generateEncryptionKey() {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        String encoded = Base64.getEncoder().encodeToString(keyBytes);
        Arrays.fill(keyBytes, (byte) 0);
        return encoded;
    }
}
