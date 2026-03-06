package com.skuri.skuri_backend.domain.board.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

    public static final String DELETED_PLACEHOLDER = "삭제된 댓글입니다";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_id", nullable = false, length = 36)
    private String authorId;

    @Column(name = "author_name", length = 50)
    private String authorName;

    @Column(name = "author_profile_image", length = 500)
    private String authorProfileImage;

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous;

    @Column(name = "anon_id", length = 100)
    private String anonId;

    @Column(name = "anonymous_order")
    private Integer anonymousOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OrderBy("createdAt ASC")
    @OneToMany(mappedBy = "parent")
    private final List<Comment> replies = new ArrayList<>();

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    private Comment(
            Post post,
            String content,
            String authorId,
            String authorName,
            String authorProfileImage,
            boolean anonymous,
            String anonId,
            Integer anonymousOrder,
            Comment parent
    ) {
        this.post = post;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorProfileImage = authorProfileImage;
        this.anonymous = anonymous;
        this.anonId = anonId;
        this.anonymousOrder = anonymousOrder;
        this.parent = parent;
        this.deleted = false;
    }

    public static Comment create(
            Post post,
            String content,
            String authorId,
            String authorName,
            String authorProfileImage,
            boolean anonymous,
            String anonId,
            Integer anonymousOrder,
            Comment parent
    ) {
        return new Comment(
                post,
                content,
                authorId,
                authorName,
                authorProfileImage,
                anonymous,
                anonId,
                anonymousOrder,
                parent
        );
    }

    public boolean isAuthor(String memberId) {
        return this.authorId.equals(memberId);
    }

    public boolean isRoot() {
        return this.parent == null;
    }

    public boolean hasParent() {
        return this.parent != null;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void softDelete() {
        this.deleted = true;
        this.content = DELETED_PLACEHOLDER;
    }
}
