package com.invoiceautomation.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Override public String convertToDatabaseColumn(List<String> value) {
        if (value == null) return null;
        try { return MAPPER.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Could not serialize string list", ex); }
    }
    @Override public List<String> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) return new ArrayList<>();
        try { return MAPPER.readValue(value, new TypeReference<List<String>>() {}); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Could not deserialize string list", ex); }
    }
}
