package com.skuri.skuri_backend.domain.notice.entity;

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
        name = "notice_comment_likes",
        indexes = {
                @Index(name = "idx_notice_comment_likes_comment_id", columnList = "comment_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeCommentLike {

    @EmbeddedId
    private NoticeCommentLikeId id;

    @MapsId("commentId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private NoticeComment comment;

    private NoticeCommentLike(NoticeComment comment, String userId) {
        this.id = NoticeCommentLikeId.of(userId, comment.getId());
        this.comment = comment;
    }

    public static NoticeCommentLike create(NoticeComment comment, String userId) {
        return new NoticeCommentLike(comment, userId);
    }
}
