package com.skuri.skuri_backend.domain.notice.repository;

import com.skuri.skuri_backend.domain.notice.entity.NoticeLike;
import com.skuri.skuri_backend.domain.notice.entity.NoticeLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NoticeLikeRepository extends JpaRepository<NoticeLike, NoticeLikeId> {

    Optional<NoticeLike> findById_UserIdAndId_NoticeId(String userId, String noticeId);

    boolean existsById_UserIdAndId_NoticeId(String userId, String noticeId);

    @Query("""
            select nl.id.noticeId
            from NoticeLike nl
            where nl.id.userId = :userId
              and nl.id.noticeId in :noticeIds
            """)
    List<String> findLikedNoticeIds(@Param("userId") String userId, @Param("noticeIds") Collection<String> noticeIds);
}
