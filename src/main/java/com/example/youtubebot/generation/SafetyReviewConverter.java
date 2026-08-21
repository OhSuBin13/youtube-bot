package com.example.youtubebot.generation;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SafetyReviewConverter implements AttributeConverter<SafetyReview, String> {

    @Override
    public String convertToDatabaseColumn(SafetyReview attribute) {
        return attribute == null ? null : attribute.databaseValue();
    }

    @Override
    public SafetyReview convertToEntityAttribute(String databaseValue) {
        return databaseValue == null ? null : SafetyReview.fromDatabaseValue(databaseValue);
    }
}
