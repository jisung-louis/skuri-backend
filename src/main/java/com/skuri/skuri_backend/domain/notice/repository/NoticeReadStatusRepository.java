package com.skuri.skuri_backend.domain.notice.repository;

import com.skuri.skuri_backend.domain.notice.entity.NoticeReadStatus;
import com.skuri.skuri_backend.domain.notice.entity.NoticeReadStatusId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NoticeReadStatusRepository extends JpaRepository<NoticeReadStatus, NoticeReadStatusId> {

    Optional<NoticeReadStatus> findById_UserIdAndId_NoticeId(String userId, String noticeId);

    boolean existsById_UserIdAndId_NoticeIdAndReadTrue(String userId, String noticeId);

    @Query("""
            select s.id.noticeId
            from NoticeReadStatus s
            where s.id.userId = :userId
              and s.id.noticeId in :noticeIds
              and s.read = true
            """)
    List<String> findReadNoticeIds(@Param("userId") String userId, @Param("noticeIds") Collection<String> noticeIds);

    long deleteById_UserId(String userId);
}
