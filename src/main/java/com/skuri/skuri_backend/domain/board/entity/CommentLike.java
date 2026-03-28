package com.skuri.skuri_backend.domain.board.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "comment_likes",
        indexes = {
                @Index(name = "idx_comment_likes_comment_id", columnList = "comment_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike {

    @EmbeddedId
    private CommentLikeId id;

    @MapsId("commentId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    private CommentLike(Comment comment, String userId) {
        this.id = CommentLikeId.of(userId, comment.getId());
        this.comment = comment;
    }

    public static CommentLike create(Comment comment, String userId) {
        return new CommentLike(comment, userId);
    }
}
