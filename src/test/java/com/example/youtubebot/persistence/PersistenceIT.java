package com.example.youtubebot.persistence;

import com.example.youtubebot.oauth.OAuthConnectionCredentials;
import com.example.youtubebot.oauth.OAuthConnectionInput;
import com.example.youtubebot.oauth.OAuthTokenStore;
import jakarta.persistence.EntityManager;
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
import org.testcontainers.utility.DockerImageName;

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
    @ServiceConnection(name = "postgres")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse(
                            "postgres:17.11-alpine3.24@sha256:18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73")
                    .asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void tokenEncryptionProperties(DynamicPropertyRegistry registry) {
        registry.add("youtube.token-encryption.key", () -> TOKEN_ENCRYPTION_KEY);
        registry.add("youtube.token-encryption.key-version", () -> 3);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

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

        VideoMetadata videoMetadata = new VideoMetadata(
                "테스트 영상",
                "테스트 영상 설명",
                List.of("테스트", "Spring"),
                "교육",
                "ko",
                "ko",
                "2026-08-20T06:00:00Z",
                "PT5M");
        ChannelContext channelContext = new ChannelContext(
                "테스트 채널",
                "테스트 채널 설명",
                List.of("개발", "Spring"),
                List.of("Technology"));
        PublicComments publicComments = new PublicComments(List.of(
                new PublicComments.PublicComment(
                        "좋은 영상입니다",
                        7,
                        "2026-08-20T06:30:00Z")));
        EvidenceFields evidenceFields = new EvidenceFields(List.of("video.title"));
        RiskTopics riskTopics = new RiskTopics(List.of(RiskTopics.RiskTopic.FINANCE));
        DuplicateCheckResult duplicateCheckResult = new DuplicateCheckResult(false, 0.25);

        videoContextRepository.saveAndFlush(new VideoContext(
                videoId,
                "https://www.youtube.com/watch?v=" + videoId,
                videoMetadata,
                channelContext,
                publicComments,
                "사용자 요약",
                now,
                now.plus(30, ChronoUnit.DAYS)));
        aiGenerationRepository.saveAndFlush(AiGeneration.create(new NewAiGeneration(
                draftId,
                videoId,
                "qwen3:4b",
                "v1",
                "유익한 영상 감사합니다.",
                evidenceFields,
                ContextStatus.SUFFICIENT,
                SafetyReview.REQUIRES_HUMAN_REVIEW,
                riskTopics,
                "영상 제목을 근거로 작성",
                duplicateCheckResult,
                now)));
        commentAttemptRepository.saveAndFlush(CommentAttempt.approved(new ApprovedCommentAttempt(
                attemptId,
                videoId,
                draftId,
                "유익한 영상 감사합니다.",
                "유익한 설명 감사합니다.",
                "UC_AUTHOR_CHANNEL",
                "UC_TARGET_CHANNEL",
                now)));
        videoCommentGuardRepository.saveAndFlush(new VideoCommentGuard(
                videoId, GuardStatus.PUBLISHING, attemptId, now, now));

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
        assertEquals("테스트 영상", jdbcTemplate.queryForObject(
                "SELECT video_metadata ->> 'title' FROM video_context WHERE video_id = ?",
                String.class,
                videoId));
        assertEquals("개발", jdbcTemplate.queryForObject(
                "SELECT channel_context -> 'keywords' ->> 0 FROM video_context WHERE video_id = ?",
                String.class,
                videoId));
        assertEquals("좋은 영상입니다", jdbcTemplate.queryForObject(
                "SELECT public_comments -> 0 ->> 'text' FROM video_context WHERE video_id = ?",
                String.class,
                videoId));
        assertEquals("video.title", jdbcTemplate.queryForObject(
                "SELECT evidence_fields ->> 0 FROM ai_generation WHERE draft_id = ?",
                String.class,
                draftId));
        assertEquals("finance", jdbcTemplate.queryForObject(
                "SELECT risk_topics ->> 0 FROM ai_generation WHERE draft_id = ?",
                String.class,
                draftId));
        assertEquals(false, jdbcTemplate.queryForObject(
                "SELECT (duplicate_check_result ->> 'duplicate')::boolean "
                        + "FROM ai_generation WHERE draft_id = ?",
                Boolean.class,
                draftId));
        assertEquals("sufficient", jdbcTemplate.queryForObject(
                "SELECT context_status FROM ai_generation WHERE draft_id = ?",
                String.class,
                draftId));
        assertEquals("requires_human_review", jdbcTemplate.queryForObject(
                "SELECT safety_review FROM ai_generation WHERE draft_id = ?",
                String.class,
                draftId));
        assertEquals(true, jdbcTemplate.queryForObject(
                "SELECT user_edited_text IS NULL AND created_at = updated_at "
                        + "FROM ai_generation WHERE draft_id = ?",
                Boolean.class,
                draftId));
        assertEquals("APPROVED", jdbcTemplate.queryForObject(
                "SELECT status FROM comment_attempt WHERE attempt_id = ?",
                String.class,
                attemptId));
        assertEquals(true, jdbcTemplate.queryForObject(
                "SELECT youtube_comment_id IS NULL AND error_code IS NULL "
                        + "AND requested_at IS NULL AND completed_at IS NULL "
                        + "FROM comment_attempt WHERE attempt_id = ?",
                Boolean.class,
                attemptId));
        assertEquals("PUBLISHING", jdbcTemplate.queryForObject(
                "SELECT status FROM video_comment_guard WHERE video_id = ?",
                String.class,
                videoId));

        entityManager.clear();
        VideoContext restoredContext = videoContextRepository.findById(videoId).orElseThrow();
        AiGeneration restoredGeneration = aiGenerationRepository.findById(draftId).orElseThrow();
        CommentAttempt restoredAttempt = commentAttemptRepository.findById(attemptId).orElseThrow();
        VideoCommentGuard restoredGuard = videoCommentGuardRepository.findById(videoId).orElseThrow();
        assertEquals(videoMetadata, restoredContext.getVideoMetadata());
        assertEquals(channelContext, restoredContext.getChannelContext());
        assertEquals(publicComments, restoredContext.getPublicComments());
        assertEquals(evidenceFields, restoredGeneration.getEvidenceFields());
        assertEquals(riskTopics, restoredGeneration.getRiskTopics());
        assertEquals(duplicateCheckResult, restoredGeneration.getDuplicateCheckResult());
        assertEquals(ContextStatus.SUFFICIENT, restoredGeneration.getContextStatus());
        assertEquals(SafetyReview.REQUIRES_HUMAN_REVIEW, restoredGeneration.getSafetyReview());
        assertEquals(CommentAttemptStatus.APPROVED, restoredAttempt.getStatus());
        assertEquals(GuardStatus.PUBLISHING, restoredGuard.getStatus());
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
