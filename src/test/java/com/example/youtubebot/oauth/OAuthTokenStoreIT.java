package com.example.youtubebot.oauth;

import com.example.youtubebot.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OAuthTokenStoreIT extends PostgreSqlIntegrationTest {

    @Autowired
    private OAuthTokenStore tokenStore;

    @Autowired
    private OAuthConnectionRepository connectionRepository;

    @Test
    void tokenStorePersistsCiphertextNonceVersionAndRoundTrips() {
        String refreshToken = "1//database-round-trip-token-한글";
        Instant connectedAt = Instant.parse("2026-08-20T07:00:00Z");
        Set<String> scopeValues = Set.of(
                GoogleOAuthProperties.YOUTUBE_SCOPE,
                "scope-b",
                "scope-a");
        OAuthConnectionInput input = new OAuthConnectionInput(
                new RefreshToken(refreshToken),
                new GrantedScopes(scopeValues),
                new YouTubeChannelIdentity("UC_AUTHOR_CHANNEL", "작성 채널"),
                connectedAt);

        tokenStore.create(input);

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT refresh_token_ciphertext, refresh_token_nonce, key_version, granted_scope
                FROM oauth_connection
                WHERE id = 1
                """);
        byte[] ciphertext = (byte[]) row.get("refresh_token_ciphertext");
        byte[] nonce = (byte[]) row.get("refresh_token_nonce");
        assertFalse(Arrays.equals(
                refreshToken.getBytes(StandardCharsets.UTF_8), ciphertext));
        assertEquals(refreshToken.getBytes(StandardCharsets.UTF_8).length + 16, ciphertext.length);
        assertEquals(12, nonce.length);
        assertEquals(3, row.get("key_version"));
        assertEquals(
                String.join(" ", new TreeSet<>(scopeValues)),
                row.get("granted_scope"));

        OAuthConnectionCredentials restored = tokenStore.find().orElseThrow();
        assertEquals(refreshToken, restored.refreshToken().value());
        assertEquals(scopeValues, restored.grantedScopes().values());
        assertEquals(
                new YouTubeChannelIdentity("UC_AUTHOR_CHANNEL", "작성 채널"),
                restored.channel());
        assertEquals(connectedAt, restored.connectedAt());
    }

    @Test
    void createNeverOverwritesTheFixedChannel() {
        Instant connectedAt = Instant.parse("2026-08-24T07:00:00Z");
        tokenStore.create(new OAuthConnectionInput(
                new RefreshToken("first-refresh-token"),
                new GrantedScopes(Set.of(GoogleOAuthProperties.YOUTUBE_SCOPE)),
                new YouTubeChannelIdentity("UC_FIRST_CHANNEL", "First Channel"),
                connectedAt));

        GoogleOAuthException exception = assertThrows(GoogleOAuthException.class, () ->
                tokenStore.create(new OAuthConnectionInput(
                        new RefreshToken("second-refresh-token"),
                        new GrantedScopes(Set.of(GoogleOAuthProperties.YOUTUBE_SCOPE)),
                        new YouTubeChannelIdentity("UC_SECOND_CHANNEL", "Second Channel"),
                        connectedAt.plusSeconds(1))));

        assertEquals(GoogleOAuthErrorCode.ALREADY_CONNECTED, exception.getErrorCode());
        OAuthConnectionCredentials stored = tokenStore.find().orElseThrow();
        assertEquals("first-refresh-token", stored.refreshToken().value());
        assertEquals(
                new YouTubeChannelIdentity("UC_FIRST_CHANNEL", "First Channel"),
                stored.channel());
    }

    @Test
    void repositoryDoesNotHideAnotherDatabaseConstraintViolationAsIdConflict() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> connectionRepository.insertIfAbsent(
                        OAuthTokenStore.SINGLETON_ID,
                        new byte[17],
                        new byte[12],
                        1,
                        GoogleOAuthProperties.YOUTUBE_SCOPE,
                        "UC_CHANNEL",
                        "x".repeat(256),
                        Instant.parse("2026-08-24T07:00:00Z")));

        assertFalse(tokenStore.exists());
    }
}
