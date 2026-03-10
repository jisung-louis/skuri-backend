package com.skuri.skuri_backend.domain.board.repository;

import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, String> {

    @Query("""
            select p.id as id,
                   p.title as title,
                   p.content as content,
                   p.authorId as authorId,
                   p.authorName as authorName,
                   p.authorProfileImage as authorProfileImage,
                   p.anonymous as anonymous,
                   p.category as category,
                   p.viewCount as viewCount,
                   p.likeCount as likeCount,
                   p.commentCount as commentCount,
                   p.pinned as pinned,
                   p.createdAt as createdAt,
                   case when exists (
                       select 1
                       from PostImage pi
                       where pi.post = p
                   ) then true else false end as hasImage
            from Post p
            where p.deleted = false
              and (:category is null or p.category = :category)
              and (:search is null
                    or lower(p.title) like lower(concat('%', :search, '%'))
                    or lower(p.content) like lower(concat('%', :search, '%')))
              and (:authorId is null or p.authorId = :authorId)
            """)
    Page<PostSummaryProjection> searchSummaries(
            @Param("category") PostCategory category,
            @Param("search") String search,
            @Param("authorId") String authorId,
            Pageable pageable
    );

    @Query("""
            select p.id as id,
                   p.title as title,
                   p.content as content,
                   p.authorId as authorId,
                   p.authorName as authorName,
                   p.authorProfileImage as authorProfileImage,
                   p.anonymous as anonymous,
                   p.category as category,
                   p.viewCount as viewCount,
                   p.likeCount as likeCount,
                   p.commentCount as commentCount,
                   p.pinned as pinned,
                   p.createdAt as createdAt,
                   case when exists (
                       select 1
                       from PostImage pi
                       where pi.post = p
                   ) then true else false end as hasImage
            from Post p
            where p.deleted = false
              and p.authorId = :authorId
            """)
    Page<PostSummaryProjection> findActiveSummariesByAuthorId(@Param("authorId") String authorId, Pageable pageable);

    @Query("""
            select p.id as id,
                   p.title as title,
                   p.content as content,
                   p.authorId as authorId,
                   p.authorName as authorName,
                   p.authorProfileImage as authorProfileImage,
                   p.anonymous as anonymous,
                   p.category as category,
                   p.viewCount as viewCount,
                   p.likeCount as likeCount,
                   p.commentCount as commentCount,
                   p.pinned as pinned,
                   p.createdAt as createdAt,
                   case when exists (
                       select 1
                       from PostImage pi
                       where pi.post = p
                   ) then true else false end as hasImage
            from Post p
            join PostInteraction pi on pi.post = p
            where p.deleted = false
              and pi.id.userId = :userId
              and pi.bookmarked = true
            """)
    Page<PostSummaryProjection> findBookmarkedSummaries(@Param("userId") String userId, Pageable pageable);

    @EntityGraph(attributePaths = "images")
    @Query("""
            select p
            from Post p
            where p.id = :postId
              and p.deleted = false
            """)
    Optional<Post> findActiveDetailById(@Param("postId") String postId);

    Optional<Post> findByIdAndDeletedFalse(String postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Post p
            where p.id = :postId
              and p.deleted = false
            """)
    Optional<Post> findActiveByIdForUpdate(@Param("postId") String postId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Post p
            set p.viewCount = p.viewCount + 1
            where p.id = :postId
              and p.deleted = false
            """)
    int incrementViewCount(@Param("postId") String postId);

    List<Post> findByAuthorId(String authorId);
}
