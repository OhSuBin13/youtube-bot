package com.example.youtubebot.persistence;

import java.util.List;
import java.util.Objects;

public record VideoMetadata(
        String title,
        String description,
        List<String> tags,
        String category,
        String defaultLanguage,
        String defaultAudioLanguage,
        String publishedAt,
        String duration) {

    public VideoMetadata {
        title = requireNonBlank(title, "title");
        description = Objects.requireNonNull(description, "description must not be null");
        tags = List.copyOf(Objects.requireNonNull(tags, "tags must not be null"));
        category = requireNonBlank(category, "category");
        publishedAt = requireNonBlank(publishedAt, "publishedAt");
        duration = requireNonBlank(duration, "duration");
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
