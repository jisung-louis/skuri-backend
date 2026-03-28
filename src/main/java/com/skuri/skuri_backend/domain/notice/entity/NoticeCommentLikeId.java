package com.skuri.skuri_backend.domain.notice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeCommentLikeId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "comment_id", length = 36)
    private String commentId;

    private NoticeCommentLikeId(String userId, String commentId) {
        this.userId = userId;
        this.commentId = commentId;
    }

    public static NoticeCommentLikeId of(String userId, String commentId) {
        return new NoticeCommentLikeId(userId, commentId);
    }
}
