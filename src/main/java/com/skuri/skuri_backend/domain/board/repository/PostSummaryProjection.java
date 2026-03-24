package com.skuri.skuri_backend.domain.board.repository;

import com.skuri.skuri_backend.domain.board.entity.PostCategory;

import java.time.LocalDateTime;

public interface PostSummaryProjection {

    String getId();

    String getTitle();

    String getContent();

    String getAuthorId();

    String getAuthorName();

    String getAuthorProfileImage();

    boolean isAnonymous();

    PostCategory getCategory();

    int getViewCount();

    int getLikeCount();

    int getCommentCount();

    int getBookmarkCount();

    boolean isPinned();

    LocalDateTime getCreatedAt();

    boolean isHasImage();
}
