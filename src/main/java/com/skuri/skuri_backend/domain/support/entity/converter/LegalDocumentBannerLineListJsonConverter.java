package com.skuri.skuri_backend.domain.support.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentBannerLine;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.List;

@Converter
public class LegalDocumentBannerLineListJsonConverter implements AttributeConverter<List<LegalDocumentBannerLine>, String> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperConfig.SHARED_OBJECT_MAPPER;
    private static final TypeReference<List<LegalDocumentBannerLine>> BANNER_LINE_LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<LegalDocumentBannerLine> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("법적 문서 배너 라인 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public List<LegalDocumentBannerLine> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(dbData);
            if (root.isTextual()) {
                return OBJECT_MAPPER.readValue(root.asText(), BANNER_LINE_LIST_TYPE);
            }
            return OBJECT_MAPPER.convertValue(root, BANNER_LINE_LIST_TYPE);
        } catch (IOException e) {
            throw new IllegalArgumentException("법적 문서 배너 라인 역직렬화에 실패했습니다.", e);
        }
    }
}
