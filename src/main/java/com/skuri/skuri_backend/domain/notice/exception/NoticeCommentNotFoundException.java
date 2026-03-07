package com.skuri.skuri_backend.domain.notice.exception;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;

public class NoticeCommentNotFoundException extends BusinessException {

    public NoticeCommentNotFoundException() {
        super(ErrorCode.NOTICE_COMMENT_NOT_FOUND);
    }
}
