package com.skuri.skuri_backend.domain.member.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Converter
public class BooleanMapJsonConverter implements AttributeConverter<Map<String, Boolean>, String> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperConfig.SHARED_OBJECT_MAPPER;
    private static final TypeReference<Map<String, Boolean>> BOOLEAN_MAP_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, Boolean> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("알림 상세 설정 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public Map<String, Boolean> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new HashMap<>();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(dbData);
            // H2/MySQL 드라이버 차이로 JSON 컬럼이 문자열(JSON-in-JSON) 형태로 들어오는 경우를 허용한다.
            if (root.isTextual()) {
                return OBJECT_MAPPER.readValue(root.asText(), BOOLEAN_MAP_TYPE);
            }
            return OBJECT_MAPPER.convertValue(root, BOOLEAN_MAP_TYPE);
        } catch (IOException e) {
            throw new IllegalArgumentException("알림 상세 설정 역직렬화에 실패했습니다.", e);
        }
    }
}
