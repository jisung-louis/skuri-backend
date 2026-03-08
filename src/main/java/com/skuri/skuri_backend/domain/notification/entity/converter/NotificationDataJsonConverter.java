package com.skuri.skuri_backend.domain.notification.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;

@Converter
public class NotificationDataJsonConverter implements AttributeConverter<NotificationData, String> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperConfig.SHARED_OBJECT_MAPPER;

    @Override
    public String convertToDatabaseColumn(NotificationData attribute) {
        NotificationData value = attribute == null ? NotificationData.empty() : attribute;
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("알림 payload 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public NotificationData convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return NotificationData.empty();
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(dbData);
            if (root.isTextual()) {
                return OBJECT_MAPPER.readValue(root.asText(), NotificationData.class);
            }
            return OBJECT_MAPPER.treeToValue(root, NotificationData.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("알림 payload 역직렬화에 실패했습니다.", e);
        }
    }
}
