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
public class PostInteractionId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "post_id", length = 36)
    private String postId;

    private PostInteractionId(String userId, String postId) {
        this.userId = userId;
        this.postId = postId;
    }

    public static PostInteractionId of(String userId, String postId) {
        return new PostInteractionId(userId, postId);
    }
}
