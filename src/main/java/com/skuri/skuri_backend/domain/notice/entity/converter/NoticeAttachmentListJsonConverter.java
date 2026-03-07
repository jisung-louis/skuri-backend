package com.skuri.skuri_backend.domain.notice.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.domain.notice.entity.NoticeAttachment;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.List;

@Converter
public class NoticeAttachmentListJsonConverter implements AttributeConverter<List<NoticeAttachment>, String> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperConfig.SHARED_OBJECT_MAPPER;
    private static final TypeReference<List<NoticeAttachment>> ATTACHMENT_LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<NoticeAttachment> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("공지 첨부파일 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public List<NoticeAttachment> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(dbData);
            if (root.isTextual()) {
                return OBJECT_MAPPER.readValue(root.asText(), ATTACHMENT_LIST_TYPE);
            }
            return OBJECT_MAPPER.convertValue(root, ATTACHMENT_LIST_TYPE);
        } catch (IOException e) {
            throw new IllegalArgumentException("공지 첨부파일 역직렬화에 실패했습니다.", e);
        }
    }
}
