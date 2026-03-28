package com.skuri.skuri_backend.domain.board.repository;

import com.skuri.skuri_backend.domain.board.entity.CommentLike;
import com.skuri.skuri_backend.domain.board.entity.CommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {

    Optional<CommentLike> findById_UserIdAndId_CommentId(String userId, String commentId);

    boolean existsById_UserIdAndId_CommentId(String userId, String commentId);

    @Query("""
            select cl.id.commentId
            from CommentLike cl
            where cl.id.userId = :userId
              and cl.id.commentId in :commentIds
            """)
    List<String> findLikedCommentIds(@Param("userId") String userId, @Param("commentIds") Collection<String> commentIds);

    List<CommentLike> findById_UserId(String userId);
}
