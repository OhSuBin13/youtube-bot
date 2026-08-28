package com.example.youtubebot.oauth;

import java.time.Instant;

public record OAuthConnectionStatus(
        boolean configured,
        boolean connected,
        String channelId,
        String channelName,
        Instant connectedAt) {

    static OAuthConnectionStatus disconnected(boolean configured) {
        return new OAuthConnectionStatus(configured, false, null, null, null);
    }

    static OAuthConnectionStatus connected(boolean configured, OAuthConnectionInfo connection) {
        return new OAuthConnectionStatus(
                configured,
                true,
                connection.channel().channelId(),
                connection.channel().channelName(),
                connection.connectedAt());
    }
}
