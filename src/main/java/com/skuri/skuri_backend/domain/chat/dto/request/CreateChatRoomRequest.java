package com.skuri.skuri_backend.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "커스텀 공개 채팅방 생성 요청")
public record CreateChatRoomRequest(
        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 100, message = "name은 100자 이하여야 합니다.")
        @Schema(description = "채팅방 이름", example = "시험기간 밤샘 메이트")
        String name,

        @Size(max = 500, message = "description은 500자 이하여야 합니다.")
        @Schema(description = "채팅방 설명", example = "기말고사 기간 같이 공부할 사람들 모여요.", nullable = true)
        String description
) {
}
