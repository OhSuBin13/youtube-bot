package com.example.youtubebot.oauth;

import java.time.Instant;
import java.util.Objects;

public record OAuthConnectionInput(
        RefreshToken refreshToken,
        GrantedScopes grantedScopes,
        YouTubeChannelIdentity channel,
        Instant connectedAt) {

    public OAuthConnectionInput {
        Objects.requireNonNull(refreshToken, "Refresh token is required");
        Objects.requireNonNull(grantedScopes, "Granted scopes are required");
        Objects.requireNonNull(channel, "YouTube channel identity is required");
        Objects.requireNonNull(connectedAt, "Connected time is required");
    }
}
