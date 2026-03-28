package com.skuri.skuri_backend.domain.board.repository;

import com.skuri.skuri_backend.domain.board.entity.Comment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from Comment c
            where c.id = :commentId
              and c.post.deleted = false
            """)
    Optional<Comment> findByIdForUpdate(@Param("commentId") String commentId);

    Optional<Comment> findFirstByPost_IdAndAnonIdAndAnonymousOrderIsNotNullOrderByCreatedAtAsc(String postId, String anonId);

    @Query("""
            select coalesce(max(c.anonymousOrder), 0)
            from Comment c
            where c.post.id = :postId
            """)
    int findMaxAnonymousOrderByPostId(@Param("postId") String postId);

    List<Comment> findByAuthorId(String authorId);

    @EntityGraph(attributePaths = {"post"})
    @Query(
            value = """
                    select c
                    from Comment c
                    where c.authorId = :authorId
                      and c.deleted = false
                      and c.post.deleted = false
                    """,
            countQuery = """
                    select count(c)
                    from Comment c
                    where c.authorId = :authorId
                      and c.deleted = false
                      and c.post.deleted = false
                    """
    )
    Page<Comment> findActiveByAuthorId(@Param("authorId") String authorId, Pageable pageable);

    @Query("""
            select distinct c.post.id
            from Comment c
            where c.authorId = :authorId
              and c.deleted = false
              and c.post.deleted = false
              and c.post.id in :postIds
            """)
    List<String> findCommentedPostIds(@Param("authorId") String authorId, @Param("postIds") Collection<String> postIds);
}
