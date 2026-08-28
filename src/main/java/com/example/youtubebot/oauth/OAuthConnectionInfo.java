package com.example.youtubebot.oauth;

import java.time.Instant;
import java.util.Objects;

public record OAuthConnectionInfo(
        YouTubeChannelIdentity channel,
        Instant connectedAt) {

    public OAuthConnectionInfo {
        Objects.requireNonNull(channel, "YouTube channel identity is required");
        Objects.requireNonNull(connectedAt, "Connected time is required");
    }
}
