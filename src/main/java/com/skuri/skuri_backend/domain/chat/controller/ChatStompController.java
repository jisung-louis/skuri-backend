package com.skuri.skuri_backend.domain.chat.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.request.SendChatMessageRequest;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;

    @MessageMapping("/chat/{chatRoomId}")
    public void sendMessage(
            @DestinationVariable String chatRoomId,
            @Valid @Payload SendChatMessageRequest request,
            Principal principal
    ) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.STOMP_AUTH_FAILED);
        }
        chatService.sendMessage(chatRoomId, principal.getName(), request);
    }
}
