package com.skuri.skuri_backend.domain.support.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentSection;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.List;

@Converter
public class LegalDocumentSectionListJsonConverter implements AttributeConverter<List<LegalDocumentSection>, String> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperConfig.SHARED_OBJECT_MAPPER;
    private static final TypeReference<List<LegalDocumentSection>> SECTION_LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<LegalDocumentSection> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("법적 문서 섹션 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public List<LegalDocumentSection> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(dbData);
            if (root.isTextual()) {
                return OBJECT_MAPPER.readValue(root.asText(), SECTION_LIST_TYPE);
            }
            return OBJECT_MAPPER.convertValue(root, SECTION_LIST_TYPE);
        } catch (IOException e) {
            throw new IllegalArgumentException("법적 문서 섹션 역직렬화에 실패했습니다.", e);
        }
    }
}
