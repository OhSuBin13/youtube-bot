package com.example.youtubebot.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;
import java.util.Objects;

public record PublicComments(List<PublicComment> values) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public PublicComments {
        values = List.copyOf(Objects.requireNonNull(values, "values must not be null"));
    }

    @Override
    @JsonValue
    public List<PublicComment> values() {
        return values;
    }

    public record PublicComment(String text, long likeCount, String publishedAt) {

        public PublicComment {
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("text must not be blank");
            }
            if (likeCount < 0) {
                throw new IllegalArgumentException("likeCount must not be negative");
            }
            if (publishedAt == null || publishedAt.isBlank()) {
                throw new IllegalArgumentException("publishedAt must not be blank");
            }
        }
    }
}
