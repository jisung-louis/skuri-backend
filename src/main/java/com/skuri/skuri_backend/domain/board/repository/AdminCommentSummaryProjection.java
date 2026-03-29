package com.skuri.skuri_backend.domain.board.repository;

import java.time.LocalDateTime;

public interface AdminCommentSummaryProjection {

    String getId();

    String getPostId();

    String getPostTitle();

    String getAuthorId();

    String getAuthorNickname();

    String getAuthorRealname();

    String getContent();

    String getParentCommentId();

    LocalDateTime getCreatedAt();

    boolean isHidden();

    boolean isDeleted();
}
