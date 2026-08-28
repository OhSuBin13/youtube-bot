package com.example.youtubebot.oauth;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

public record OAuthFlowState(
        String state,
        String codeVerifier,
        Instant initiatedAt) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
