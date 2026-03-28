package com.skuri.skuri_backend.domain.board.entity;

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
public class CommentLikeId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "comment_id", length = 36)
    private String commentId;

    private CommentLikeId(String userId, String commentId) {
        this.userId = userId;
        this.commentId = commentId;
    }

    public static CommentLikeId of(String userId, String commentId) {
        return new CommentLikeId(userId, commentId);
    }
}
