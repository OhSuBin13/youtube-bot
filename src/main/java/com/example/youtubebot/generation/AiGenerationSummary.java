package com.example.youtubebot.generation;

import java.time.Instant;
import java.util.UUID;

public interface AiGenerationSummary {

    UUID getDraftId();

    String getAiOriginalText();

    String getUserEditedText();

    ContextStatus getContextStatus();

    SafetyReview getSafetyReview();

    Instant getCreatedAt();

    Instant getUpdatedAt();
}
