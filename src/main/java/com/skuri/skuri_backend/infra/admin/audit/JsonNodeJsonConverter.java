package com.skuri.skuri_backend.infra.admin.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;

@Converter
public class JsonNodeJsonConverter implements AttributeConverter<JsonNode, String> {

    private static final ObjectMapper OBJECT_MAPPER = AdminAuditObjectMapper.OBJECT_MAPPER;

    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        if (attribute == null || attribute.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("감사 로그 JSON 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public JsonNode convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(dbData);
            if (node != null && node.isTextual()) {
                String nestedJson = node.asText();
                if (nestedJson.startsWith("{") || nestedJson.startsWith("[")) {
                    return OBJECT_MAPPER.readTree(nestedJson);
                }
            }
            return node;
        } catch (IOException e) {
            throw new IllegalArgumentException("감사 로그 JSON 역직렬화에 실패했습니다.", e);
        }
    }
}
