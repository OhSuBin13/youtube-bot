package com.example.youtubebot.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record NewAiGeneration(
        UUID draftId,
        String videoId,
        String modelName,
        String promptVersion,
        String aiOriginalText,
        EvidenceFields evidenceFields,
        ContextStatus contextStatus,
        SafetyReview safetyReview,
        RiskTopics riskTopics,
        String generationNote,
        DuplicateCheckResult duplicateCheckResult,
        Instant createdAt) {

    public NewAiGeneration {
        Objects.requireNonNull(draftId, "draftId must not be null");
        requireVideoId(videoId);
        requireText(modelName, "modelName", 100);
        requireText(promptVersion, "promptVersion", 50);
        requireText(aiOriginalText, "aiOriginalText", 200);
        Objects.requireNonNull(evidenceFields, "evidenceFields must not be null");
        Objects.requireNonNull(contextStatus, "contextStatus must not be null");
        Objects.requireNonNull(safetyReview, "safetyReview must not be null");
        Objects.requireNonNull(riskTopics, "riskTopics must not be null");
        if (generationNote == null || generationNote.isBlank()) {
            throw new IllegalArgumentException("generationNote must be non-blank");
        }
        Objects.requireNonNull(duplicateCheckResult, "duplicateCheckResult must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    private static void requireVideoId(String videoId) {
        if (videoId == null || !videoId.matches("[A-Za-z0-9_-]{11}")) {
            throw new IllegalArgumentException("videoId must be an 11-character YouTube video ID");
        }
    }

    private static void requireText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()
                || value.codePointCount(0, value.length()) > maxLength) {
            throw new IllegalArgumentException(
                    name + " must be non-blank and at most " + maxLength + " characters");
        }
    }
}
