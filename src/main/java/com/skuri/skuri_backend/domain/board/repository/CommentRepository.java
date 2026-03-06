package com.skuri.skuri_backend.domain.board.repository;

import com.skuri.skuri_backend.domain.board.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, String> {

    @Query("""
            select c
            from Comment c
            where c.post.id = :postId
            order by c.createdAt asc
            """)
    List<Comment> findByPostIdOrderByCreatedAtAsc(@Param("postId") String postId);

    @Query("""
            select c
            from Comment c
            where c.id = :commentId
              and c.post.id = :postId
              and c.post.deleted = false
            """)
    Optional<Comment> findByIdAndPostId(@Param("commentId") String commentId, @Param("postId") String postId);

    @Query("""
            select c
            from Comment c
            where c.id = :commentId
              and c.post.deleted = false
            """)
    Optional<Comment> findActiveById(@Param("commentId") String commentId);

    Optional<Comment> findFirstByPost_IdAndAnonIdAndAnonymousOrderIsNotNullOrderByCreatedAtAsc(String postId, String anonId);

    @Query("""
            select coalesce(max(c.anonymousOrder), 0)
            from Comment c
            where c.post.id = :postId
            """)
    int findMaxAnonymousOrderByPostId(@Param("postId") String postId);
}
