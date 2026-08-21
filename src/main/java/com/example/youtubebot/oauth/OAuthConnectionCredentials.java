package com.example.youtubebot.oauth;

import java.time.Instant;
import java.util.Set;

public record OAuthConnectionCredentials(
        String refreshToken,
        Set<String> grantedScopes,
        String channelId,
        String channelName,
        Instant connectedAt) {

    public OAuthConnectionCredentials {
        grantedScopes = Set.copyOf(grantedScopes);
    }
}
