package com.example.youtubebot.generation;

public enum ContextStatus {
    SUFFICIENT("sufficient"),
    INSUFFICIENT("insufficient");

    private final String databaseValue;

    ContextStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    String databaseValue() {
        return databaseValue;
    }

    static ContextStatus fromDatabaseValue(String databaseValue) {
        for (ContextStatus status : values()) {
            if (status.databaseValue.equals(databaseValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown context status: " + databaseValue);
    }
}
