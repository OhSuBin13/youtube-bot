package com.example.youtubebot;

import com.example.youtubebot.support.PostgreSqlIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FlywaySchemaIT extends PostgreSqlIntegrationTest {

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
}
