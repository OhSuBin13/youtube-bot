package com.example.youtubebot.persistence;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CreationInputTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");

    @Test
    void newAiGenerationRejectsInvalidVideoId() {
        assertThrows(IllegalArgumentException.class, () -> new NewAiGeneration(
                UUID.randomUUID(),
                "too-short",
                "qwen3:4b",
                "v1",
                "AI original text",
                new EvidenceFields(List.of("video.title")),
                ContextStatus.SUFFICIENT,
                SafetyReview.PASSED,
                new RiskTopics(List.of()),
                "Generated from the video title",
                new DuplicateCheckResult(false, 0.1),
                NOW));
    }

    @Test
    void approvedCommentAttemptRejectsBlankApprovedText() {
        assertThrows(IllegalArgumentException.class, () -> new ApprovedCommentAttempt(
                UUID.randomUUID(),
                "dQw4w9WgXcQ",
                UUID.randomUUID(),
                "AI original text",
                " ",
                "UC_AUTHOR_CHANNEL",
                "UC_TARGET_CHANNEL",
                NOW));
    }
}
