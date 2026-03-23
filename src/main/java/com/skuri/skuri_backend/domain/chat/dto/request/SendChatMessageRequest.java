package com.skuri.skuri_backend.domain.chat.dto.request;

import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "채팅 메시지 전송 요청")
public record SendChatMessageRequest(
        @NotNull
        @Schema(
                description = "클라이언트 전송 가능 메시지 타입(TEXT/IMAGE/ACCOUNT). SYSTEM/ARRIVED/END는 서버에서만 생성됩니다.",
                example = "TEXT",
                allowableValues = {"TEXT", "IMAGE", "ACCOUNT"}
        )
        ChatMessageType type,
        @Schema(description = "텍스트 메시지 본문", example = "안녕하세요!", nullable = true)
        String text,
        @Schema(description = "이미지 URL(IMAGE 타입에서 사용)", example = "https://cdn.skuri.app/chat/2026/03/05/image-1.jpg", nullable = true)
        String imageUrl,
        @Valid
        @Schema(description = "ACCOUNT 메시지 payload", nullable = true)
        AccountPayload account
) {

    @Schema(description = "ACCOUNT 메시지용 계좌 snapshot")
    public record AccountPayload(
            @Schema(description = "은행명", example = "카카오뱅크")
            @NotBlank(message = "account.bankName은 필수입니다.")
            @Size(max = 20, message = "account.bankName은 20자 이하여야 합니다.")
            String bankName,
            @Schema(description = "계좌번호", example = "3333-01-1234567")
            @NotBlank(message = "account.accountNumber는 필수입니다.")
            @Size(max = 30, message = "account.accountNumber는 30자 이하여야 합니다.")
            String accountNumber,
            @Schema(description = "예금주", example = "홍길동")
            @NotBlank(message = "account.accountHolder는 필수입니다.")
            @Size(max = 50, message = "account.accountHolder는 50자 이하여야 합니다.")
            String accountHolder,
            @Schema(description = "이름 일부 숨김 여부", example = "true", nullable = true)
            Boolean hideName,
            @Schema(description = "true면 member profile bank account도 함께 저장", example = "false", nullable = true)
            Boolean remember
    ) {
    }
}
