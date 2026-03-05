package com.skuri.skuri_backend.domain.chat.dto.request;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자 공개 채팅방 생성 요청")
public record AdminCreateChatRoomRequest(
        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 100, message = "name은 100자 이하여야 합니다.")
        @Schema(description = "채팅방 이름", example = "성결대 전체 채팅방")
        String name,
        @NotNull(message = "type은 필수입니다.")
        @Schema(
                description = "채팅방 타입 (PARTY는 관리자 생성 불가)",
                example = "UNIVERSITY",
                allowableValues = {"UNIVERSITY", "DEPARTMENT", "GAME", "CUSTOM", "PARTY"}
        )
        ChatRoomType type,
        @Size(max = 500, message = "description은 500자 이하여야 합니다.")
        @Schema(description = "채팅방 설명", example = "성결대학교 학생들의 소통 공간", nullable = true)
        String description,
        @NotNull(message = "isPublic은 필수입니다.")
        @Schema(description = "공개 여부 (관리자 생성은 true만 허용)", example = "true")
        Boolean isPublic
) {
}

