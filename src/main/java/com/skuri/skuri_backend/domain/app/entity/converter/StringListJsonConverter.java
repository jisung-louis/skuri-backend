package com.skuri.skuri_backend.domain.app.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.List;

@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperConfig.SHARED_OBJECT_MAPPER;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("앱 공지 이미지 URL 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(dbData);
            if (root.isTextual()) {
                return OBJECT_MAPPER.readValue(root.asText(), STRING_LIST_TYPE);
            }
            return OBJECT_MAPPER.convertValue(root, STRING_LIST_TYPE);
        } catch (IOException e) {
            throw new IllegalArgumentException("앱 공지 이미지 URL 역직렬화에 실패했습니다.", e);
        }
    }
}
