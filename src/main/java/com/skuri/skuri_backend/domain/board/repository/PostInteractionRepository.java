package com.skuri.skuri_backend.domain.board.repository;

import com.skuri.skuri_backend.domain.board.entity.PostInteraction;
import com.skuri.skuri_backend.domain.board.entity.PostInteractionId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostInteractionRepository extends JpaRepository<PostInteraction, PostInteractionId> {

    Optional<PostInteraction> findById_UserIdAndId_PostId(String userId, String postId);

    boolean existsById_UserIdAndId_PostIdAndLikedTrue(String userId, String postId);

    boolean existsById_UserIdAndId_PostIdAndBookmarkedTrue(String userId, String postId);

    @Query("""
            select pi.id.userId
            from PostInteraction pi
            where pi.id.postId = :postId
              and pi.bookmarked = true
            """)
    List<String> findBookmarkedUserIdsByPostId(@Param("postId") String postId);
}
