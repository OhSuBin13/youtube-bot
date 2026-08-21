package com.example.youtubebot.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record RiskTopics(List<RiskTopic> values) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public RiskTopics {
        values = List.copyOf(Objects.requireNonNull(values, "values must not be null"));
        if (values.stream().distinct().count() != values.size()) {
            throw new IllegalArgumentException("risk topics must not contain duplicates");
        }
    }

    @Override
    @JsonValue
    public List<RiskTopic> values() {
        return values;
    }

    public enum RiskTopic {
        POLITICS,
        HEALTH,
        FINANCE,
        LEGAL,
        OTHER;

        @JsonCreator
        public static RiskTopic fromJson(String value) {
            Objects.requireNonNull(value, "risk topic must not be null");
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unknown risk topic: " + value, exception);
            }
        }

        @JsonValue
        public String jsonValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
