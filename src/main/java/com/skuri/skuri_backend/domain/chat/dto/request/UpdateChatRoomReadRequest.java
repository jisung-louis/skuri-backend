package com.skuri.skuri_backend.domain.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

@Schema(description = "채팅방 읽음 처리 요청")
public record UpdateChatRoomReadRequest(
        @NotNull
        @Schema(
                description = "마지막으로 읽은 시각. timezone 없는 값은 Asia/Seoul 기준으로 해석하고, Z/offset 값은 해당 절대 시각으로 해석합니다.",
                example = "2026-03-25T21:36:29.837407"
        )
        Instant lastReadAt
) {

    private static final ZoneId CHAT_TIME_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter CHAT_READ_AT_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static UpdateChatRoomReadRequest of(@JsonProperty("lastReadAt") String lastReadAt) {
        return new UpdateChatRoomReadRequest(parseLastReadAt(lastReadAt));
    }

    private static Instant parseLastReadAt(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        String value = raw.trim();
        try {
            TemporalAccessor parsed = CHAT_READ_AT_FORMATTER.parseBest(
                    value,
                    OffsetDateTime::from,
                    LocalDateTime::from
            );
            if (parsed instanceof OffsetDateTime offsetDateTime) {
                return offsetDateTime.toInstant();
            }
            return ((LocalDateTime) parsed).atZone(CHAT_TIME_ZONE).toInstant();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "lastReadAt must be an ISO-8601 datetime. Timezone-less values use Asia/Seoul and Z/offset values keep their absolute instant.",
                    ex
            );
        }
    }
}
