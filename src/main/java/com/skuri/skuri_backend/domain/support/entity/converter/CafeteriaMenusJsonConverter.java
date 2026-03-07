package com.skuri.skuri_backend.domain.support.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Converter
public class CafeteriaMenusJsonConverter implements AttributeConverter<Map<String, Map<String, List<String>>>, String> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperConfig.SHARED_OBJECT_MAPPER;
    private static final TypeReference<Map<String, Map<String, List<String>>>> MENUS_TYPE =
            new TypeReference<>() {
            };

    @Override
    public String convertToDatabaseColumn(Map<String, Map<String, List<String>>> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("학식 메뉴 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public Map<String, Map<String, List<String>>> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new java.util.LinkedHashMap<>();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(dbData);
            if (root.isTextual()) {
                return OBJECT_MAPPER.readValue(root.asText(), MENUS_TYPE);
            }
            return OBJECT_MAPPER.convertValue(root, MENUS_TYPE);
        } catch (IOException e) {
            throw new IllegalArgumentException("학식 메뉴 역직렬화에 실패했습니다.", e);
        }
    }
}
