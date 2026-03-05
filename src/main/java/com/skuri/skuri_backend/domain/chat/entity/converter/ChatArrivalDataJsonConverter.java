package com.skuri.skuri_backend.domain.chat.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.domain.chat.entity.ChatArrivalData;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ChatArrivalDataJsonConverter implements AttributeConverter<ChatArrivalData, String> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperConfig.SHARED_OBJECT_MAPPER;

    @Override
    public String convertToDatabaseColumn(ChatArrivalData attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("도착 메시지 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public ChatArrivalData convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, ChatArrivalData.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("도착 메시지 역직렬화에 실패했습니다.", e);
        }
    }
}
