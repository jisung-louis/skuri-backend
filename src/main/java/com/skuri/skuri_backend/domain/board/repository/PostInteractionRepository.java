package com.skuri.skuri_backend.domain.board.repository;

import com.skuri.skuri_backend.domain.board.entity.PostInteraction;
import com.skuri.skuri_backend.domain.board.entity.PostInteractionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostInteractionRepository extends JpaRepository<PostInteraction, PostInteractionId> {

    Optional<PostInteraction> findById_UserIdAndId_PostId(String userId, String postId);

    boolean existsById_UserIdAndId_PostIdAndLikedTrue(String userId, String postId);

    boolean existsById_UserIdAndId_PostIdAndBookmarkedTrue(String userId, String postId);
}
