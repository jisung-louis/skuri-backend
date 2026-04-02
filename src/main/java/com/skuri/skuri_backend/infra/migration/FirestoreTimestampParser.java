package com.skuri.skuri_backend.infra.migration;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Component
public class FirestoreTimestampParser {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    public LocalDateTime toLocalDateTime(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isNumber()) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(node.longValue()), SEOUL_ZONE);
        }
        JsonNode secondsNode = node.get("_seconds");
        if (secondsNode == null || secondsNode.isNull()) {
            return null;
        }
        long seconds = secondsNode.longValue();
        int nanos = 0;
        JsonNode nanosNode = node.get("_nanoseconds");
        if (nanosNode != null && !nanosNode.isNull()) {
            nanos = nanosNode.intValue();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds, nanos), SEOUL_ZONE);
    }

    public Instant toInstant(JsonNode node) {
        LocalDateTime localDateTime = toLocalDateTime(node);
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.toInstant(ZoneOffset.ofHours(9));
    }
}
