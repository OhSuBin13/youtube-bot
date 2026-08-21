package com.example.youtubebot.persistence;

public record DuplicateCheckResult(boolean duplicate, double highestSimilarity) {

    public DuplicateCheckResult {
        if (!Double.isFinite(highestSimilarity)
                || highestSimilarity < 0.0
                || highestSimilarity > 1.0) {
            throw new IllegalArgumentException("highestSimilarity must be between 0.0 and 1.0");
        }
    }
}
