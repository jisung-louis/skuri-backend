package com.skuri.skuri_backend.domain.board.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.domain.member.entity.MemberWithdrawalSanitizer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 200)
    private String title;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostCategory category;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "bookmark_count", nullable = false)
    private int bookmarkCount;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "last_comment_at")
    private LocalDateTime lastCommentAt;

    @OrderBy("sortOrder ASC, id ASC")
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PostImage> images = new ArrayList<>();

    private Post(
            String title,
            String content,
            String authorId,
            String authorName,
            String authorProfileImage,
            boolean anonymous,
            PostCategory category
    ) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorProfileImage = authorProfileImage;
        this.anonymous = anonymous;
        this.category = category;
        this.viewCount = 0;
        this.likeCount = 0;
        this.commentCount = 0;
        this.bookmarkCount = 0;
        this.pinned = false;
        this.deleted = false;
    }

    public static Post create(
            String title,
            String content,
            String authorId,
            String authorName,
            String authorProfileImage,
            boolean anonymous,
            PostCategory category
    ) {
        return new Post(title, content, authorId, authorName, authorProfileImage, anonymous, category);
    }

    public boolean isAuthor(String memberId) {
        return this.authorId.equals(memberId);
    }

    public void update(String title, String content, PostCategory category) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (category != null) {
            this.category = category;
        }
    }

    public void markDeleted() {
        this.deleted = true;
    }

    public void appendImage(
            String url,
            String thumbUrl,
            Integer width,
            Integer height,
            Integer size,
            String mime,
            Integer sortOrder
    ) {
        this.images.add(PostImage.create(this, url, thumbUrl, width, height, size, mime, sortOrder));
    }

    public void assignAnonId() {
        if (this.anonymous && this.id != null) {
            this.anonId = this.id + ":" + this.authorId;
        }
    }

    public void anonymizeAuthor() {
        this.authorId = MemberWithdrawalSanitizer.WITHDRAWN_AUTHOR_ID;
        this.authorName = MemberWithdrawalSanitizer.WITHDRAWN_DISPLAY_NAME;
        this.authorProfileImage = null;
        this.anonId = null;
    }

    public void incrementViewCount() {
        this.viewCount += 1;
    }

    public void increaseLikeCount(int delta) {
        this.likeCount = Math.max(0, this.likeCount + delta);
    }

    public void increaseBookmarkCount(int delta) {
        this.bookmarkCount = Math.max(0, this.bookmarkCount + delta);
    }

    public void increaseCommentCount(int delta) {
        this.commentCount = Math.max(0, this.commentCount + delta);
    }

    public void updateLastCommentAt(LocalDateTime at) {
        this.lastCommentAt = at;
    }
}
