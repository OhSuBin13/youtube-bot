package com.example.youtubebot.generation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;
import java.util.Objects;

public record EvidenceFields(List<String> values) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public EvidenceFields {
        values = List.copyOf(Objects.requireNonNull(values, "values must not be null"));
        if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("evidence fields must not contain blank values");
        }
    }

    @Override
    @JsonValue
    public List<String> values() {
        return values;
    }
}
