package com.skuri.skuri_backend.domain.campus.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;

@Converter
public class CampusBannerActionParamsJsonConverter implements AttributeConverter<JsonNode, String> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperConfig.SHARED_OBJECT_MAPPER;

    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        if (attribute == null || attribute.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("캠퍼스 배너 actionParams 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public JsonNode convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(dbData);
            if (root != null && root.isTextual()) {
                String nestedJson = root.asText();
                if (nestedJson.startsWith("{") || nestedJson.startsWith("[")) {
                    return OBJECT_MAPPER.readTree(nestedJson);
                }
            }
            return root;
        } catch (IOException e) {
            throw new IllegalArgumentException("캠퍼스 배너 actionParams 역직렬화에 실패했습니다.", e);
        }
    }
}
