package com.example.youtubebot.oauth;

import java.time.Instant;
import java.util.Set;

public record OAuthConnectionInput(
        String refreshToken,
        Set<String> grantedScopes,
        String channelId,
        String channelName,
        Instant connectedAt) {

    public OAuthConnectionInput {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token must not be blank");
        }
        if (grantedScopes == null || grantedScopes.isEmpty()
                || grantedScopes.stream().anyMatch(scope ->
                        scope == null || scope.isBlank() || scope.chars().anyMatch(Character::isWhitespace))) {
            throw new IllegalArgumentException(
                    "At least one non-blank, whitespace-free granted scope is required");
        }
        grantedScopes = Set.copyOf(grantedScopes);
        if (channelId == null || channelId.isBlank()) {
            throw new IllegalArgumentException("Channel ID must not be blank");
        }
        if (channelName == null || channelName.isBlank()) {
            throw new IllegalArgumentException("Channel name must not be blank");
        }
        if (connectedAt == null) {
            throw new IllegalArgumentException("Connected time is required");
        }
    }
}
