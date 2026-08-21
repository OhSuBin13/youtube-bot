package com.example.youtubebot.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ContextStatusConverter implements AttributeConverter<ContextStatus, String> {

    @Override
    public String convertToDatabaseColumn(ContextStatus attribute) {
        return attribute == null ? null : attribute.databaseValue();
    }

    @Override
    public ContextStatus convertToEntityAttribute(String databaseValue) {
        return databaseValue == null ? null : ContextStatus.fromDatabaseValue(databaseValue);
    }
}
