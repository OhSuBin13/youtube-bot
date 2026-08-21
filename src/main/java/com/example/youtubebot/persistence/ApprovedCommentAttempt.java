package com.example.youtubebot.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ApprovedCommentAttempt(
        UUID attemptId,
        String videoId,
        UUID draftId,
        String aiGeneratedText,
        String approvedText,
        String authorChannelId,
        String targetChannelId,
        Instant approvedAt) {

    public ApprovedCommentAttempt {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        if (videoId == null || !videoId.matches("[A-Za-z0-9_-]{11}")) {
            throw new IllegalArgumentException("videoId must be an 11-character YouTube video ID");
        }
        Objects.requireNonNull(draftId, "draftId must not be null");
        requireText(aiGeneratedText, "aiGeneratedText", 200);
        requireText(approvedText, "approvedText", 200);
        requireText(authorChannelId, "authorChannelId", 64);
        requireText(targetChannelId, "targetChannelId", 64);
        Objects.requireNonNull(approvedAt, "approvedAt must not be null");
    }

    private static void requireText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()
                || value.codePointCount(0, value.length()) > maxLength) {
            throw new IllegalArgumentException(
                    name + " must be non-blank and at most " + maxLength + " characters");
        }
    }
}
