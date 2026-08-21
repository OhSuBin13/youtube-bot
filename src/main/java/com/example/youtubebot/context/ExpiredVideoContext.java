package com.example.youtubebot.context;

import java.time.Instant;

public interface ExpiredVideoContext {

    String getVideoId();

    Instant getExpiresAt();
}
