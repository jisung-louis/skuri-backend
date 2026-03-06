package com.skuri.skuri_backend.domain.board.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post_interactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostInteraction extends BaseTimeEntity {

    @EmbeddedId
    private PostInteractionId id;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "is_liked", nullable = false)
    private boolean liked;

    @Column(name = "is_bookmarked", nullable = false)
    private boolean bookmarked;

    private PostInteraction(Post post, String userId) {
        this.id = PostInteractionId.of(userId, null);
        this.post = post;
        this.liked = false;
        this.bookmarked = false;
    }

    public static PostInteraction create(Post post, String userId) {
        return new PostInteraction(post, userId);
    }

    public String getUserId() {
        return id.getUserId();
    }

    public String getPostId() {
        return id.getPostId();
    }

    public boolean like() {
        if (this.liked) {
            return false;
        }
        this.liked = true;
        return true;
    }

    public boolean unlike() {
        if (!this.liked) {
            return false;
        }
        this.liked = false;
        return true;
    }

    public boolean bookmark() {
        if (this.bookmarked) {
            return false;
        }
        this.bookmarked = true;
        return true;
    }

    public boolean unbookmark() {
        if (!this.bookmarked) {
            return false;
        }
        this.bookmarked = false;
        return true;
    }
}
