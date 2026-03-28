package com.skuri.skuri_backend.domain.notice.repository;

import com.skuri.skuri_backend.domain.notice.entity.NoticeCommentLike;
import com.skuri.skuri_backend.domain.notice.entity.NoticeCommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NoticeCommentLikeRepository extends JpaRepository<NoticeCommentLike, NoticeCommentLikeId> {

    Optional<NoticeCommentLike> findById_UserIdAndId_CommentId(String userId, String commentId);

    boolean existsById_UserIdAndId_CommentId(String userId, String commentId);

    @Query("""
            select ncl.id.commentId
            from NoticeCommentLike ncl
            where ncl.id.userId = :userId
              and ncl.id.commentId in :commentIds
            """)
    List<String> findLikedCommentIds(@Param("userId") String userId, @Param("commentIds") Collection<String> commentIds);

    List<NoticeCommentLike> findById_UserId(String userId);
}
