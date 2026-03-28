package com.skuri.skuri_backend.domain.chat.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class ChatMessageNotFoundException extends BusinessException {

    public ChatMessageNotFoundException() {
        super(ErrorCode.CHAT_MESSAGE_NOT_FOUND);
    }
}
