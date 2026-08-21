package com.example.youtubebot.persistence;

public enum SafetyReview {
    PASSED("passed"),
    REQUIRES_HUMAN_REVIEW("requires_human_review"),
    REJECTED("rejected");

    private final String databaseValue;

    SafetyReview(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    String databaseValue() {
        return databaseValue;
    }

    static SafetyReview fromDatabaseValue(String databaseValue) {
        for (SafetyReview review : values()) {
            if (review.databaseValue.equals(databaseValue)) {
                return review;
            }
        }
        throw new IllegalArgumentException("Unknown safety review: " + databaseValue);
    }
}
