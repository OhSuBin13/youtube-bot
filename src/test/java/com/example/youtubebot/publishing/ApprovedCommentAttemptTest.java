package com.example.youtubebot.publishing;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ApprovedCommentAttemptTest {

    @Test
    void rejectsBlankApprovedText() {
        assertThrows(IllegalArgumentException.class, () -> new ApprovedCommentAttempt(
                UUID.randomUUID(),
                "dQw4w9WgXcQ",
                UUID.randomUUID(),
                "AI original text",
                " ",
                "UC_AUTHOR_CHANNEL",
                "UC_TARGET_CHANNEL",
                Instant.parse("2026-08-21T00:00:00Z")));
    }
}
