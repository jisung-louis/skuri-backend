package com.skuri.skuri_backend.domain.notice.repository;

import com.skuri.skuri_backend.domain.notice.entity.Notice;
import com.skuri.skuri_backend.domain.notice.entity.NoticeBookmark;
import com.skuri.skuri_backend.domain.notice.entity.NoticeBookmarkId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NoticeBookmarkRepository extends JpaRepository<NoticeBookmark, NoticeBookmarkId> {

    Optional<NoticeBookmark> findById_UserIdAndId_NoticeId(String userId, String noticeId);

    boolean existsById_UserIdAndId_NoticeId(String userId, String noticeId);

    @Query("""
            select nb.id.noticeId
            from NoticeBookmark nb
            where nb.id.userId = :userId
              and nb.id.noticeId in :noticeIds
            """)
    List<String> findBookmarkedNoticeIds(@Param("userId") String userId, @Param("noticeIds") Collection<String> noticeIds);

    List<NoticeBookmark> findById_UserId(String userId);

    @Query(
            value = """
                    select n
                    from Notice n
                    where exists (
                        select 1
                        from NoticeBookmark nb
                        where nb.notice = n
                          and nb.id.userId = :userId
                    )
                    """,
            countQuery = """
                    select count(n)
                    from Notice n
                    where exists (
                        select 1
                        from NoticeBookmark nb
                        where nb.notice = n
                          and nb.id.userId = :userId
                    )
                    """
    )
    Page<Notice> findBookmarkedNotices(@Param("userId") String userId, Pageable pageable);

    long deleteById_UserId(String userId);
}
