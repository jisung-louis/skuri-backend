package com.skuri.skuri_backend.domain.board.constant;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.Post;

import java.util.Locale;

public enum BoardModerationStatus {

    VISIBLE,
    HIDDEN,
    DELETED;

    public static BoardModerationStatus fromPost(Post post) {
        return from(post.isHidden(), post.isDeleted());
    }

    public static BoardModerationStatus fromComment(Comment comment) {
        return from(comment.isHidden(), comment.isDeleted());
    }

    public static BoardModerationStatus from(boolean hidden, boolean deleted) {
        if (deleted) {
            return DELETED;
        }
        if (hidden) {
            return HIDDEN;
        }
        return VISIBLE;
    }

    public static BoardModerationStatus parseNullable(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parse(raw, fieldName);
    }

    public static BoardModerationStatus parse(String raw, String fieldName) {
        try {
            return BoardModerationStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    fieldName + "는 VISIBLE, HIDDEN, DELETED 중 하나여야 합니다."
            );
        }
    }

}
