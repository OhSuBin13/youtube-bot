package com.example.youtubebot.oauth;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OAuthValueObjectsTest {

    @Test
    void channelIdentityOwnsBlankAndLengthRules() {
        assertErrorCode(GoogleOAuthErrorCode.INVALID_CHANNEL, () ->
                new YouTubeChannelIdentity(" ", "Channel"));
        assertErrorCode(GoogleOAuthErrorCode.INVALID_CHANNEL, () ->
                new YouTubeChannelIdentity("x".repeat(65), "Channel"));
        assertErrorCode(GoogleOAuthErrorCode.INVALID_CHANNEL, () ->
                new YouTubeChannelIdentity("UC_CHANNEL", " "));
        assertErrorCode(GoogleOAuthErrorCode.INVALID_CHANNEL, () ->
                new YouTubeChannelIdentity("UC_CHANNEL", "x".repeat(256)));

        assertDoesNotThrow(() -> new YouTubeChannelIdentity(
                "x".repeat(YouTubeChannelIdentity.MAX_CHANNEL_ID_LENGTH),
                "x".repeat(YouTubeChannelIdentity.MAX_CHANNEL_NAME_LENGTH)));
    }

    @Test
    void grantedScopesOwnStructureAndRequiredScopeRules() {
        assertErrorCode(GoogleOAuthErrorCode.REQUIRED_SCOPE_MISSING, () ->
                new GrantedScopes(Set.of()));
        assertErrorCode(GoogleOAuthErrorCode.REQUIRED_SCOPE_MISSING, () ->
                new GrantedScopes(Set.of("another-scope")));
        assertErrorCode(GoogleOAuthErrorCode.REQUIRED_SCOPE_MISSING, () ->
                new GrantedScopes(Set.of(
                        GoogleOAuthProperties.YOUTUBE_SCOPE,
                        "invalid scope")));

        Set<String> mutableScopes = new HashSet<>();
        mutableScopes.add(GoogleOAuthProperties.YOUTUBE_SCOPE);
        GrantedScopes grantedScopes = new GrantedScopes(mutableScopes);
        mutableScopes.add("later-added-scope");

        assertEquals(Set.of(GoogleOAuthProperties.YOUTUBE_SCOPE), grantedScopes.values());
    }

    @Test
    void refreshTokenRejectsBlankAndRedactsItsStringRepresentation() {
        assertErrorCode(GoogleOAuthErrorCode.MISSING_REFRESH_TOKEN, () ->
                new RefreshToken(" "));

        RefreshToken refreshToken = new RefreshToken("secret-refresh-token");

        assertFalse(refreshToken.toString().contains(refreshToken.value()));
        assertEquals("RefreshToken[REDACTED]", refreshToken.toString());
    }

    private void assertErrorCode(
            GoogleOAuthErrorCode expected,
            org.junit.jupiter.api.function.Executable executable) {
        GoogleOAuthException exception = assertThrows(GoogleOAuthException.class, executable);
        assertEquals(expected, exception.getErrorCode());
    }
}
