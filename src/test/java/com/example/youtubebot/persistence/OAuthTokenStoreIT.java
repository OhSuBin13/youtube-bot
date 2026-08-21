package com.example.youtubebot.persistence;

import com.example.youtubebot.oauth.OAuthConnectionCredentials;
import com.example.youtubebot.oauth.OAuthConnectionInput;
import com.example.youtubebot.oauth.OAuthTokenStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OAuthTokenStoreIT extends PostgreSqlIntegrationTest {

    @Autowired
    private OAuthTokenStore tokenStore;

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
}
