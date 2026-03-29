package com.skuri.skuri_backend.domain.board.repository;

import com.skuri.skuri_backend.domain.board.entity.PostCategory;

import java.time.LocalDateTime;

public interface AdminPostSummaryProjection {

    String getId();

    PostCategory getCategory();

    String getTitle();

    String getAuthorId();

    String getAuthorNickname();

    String getAuthorRealname();

    boolean isAnonymous();

    int getCommentCount();

    int getLikeCount();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();

    boolean isHidden();

    boolean isDeleted();
}
