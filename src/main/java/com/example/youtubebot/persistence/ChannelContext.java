package com.example.youtubebot.persistence;

import java.util.List;
import java.util.Objects;

public record ChannelContext(
        String title,
        String description,
        List<String> keywords,
        List<String> topics) {

    public ChannelContext {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        description = Objects.requireNonNull(description, "description must not be null");
        keywords = List.copyOf(Objects.requireNonNull(keywords, "keywords must not be null"));
        topics = List.copyOf(Objects.requireNonNull(topics, "topics must not be null"));
    }
}
