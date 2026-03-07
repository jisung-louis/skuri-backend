package com.skuri.skuri_backend.domain.notice.repository;

import com.skuri.skuri_backend.domain.notice.entity.Notice;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, String> {

    @Query("""
            select n
            from Notice n
            where (:category is null or n.category = :category)
              and (:search is null
                    or lower(n.title) like lower(concat('%', :search, '%'))
                    or lower(coalesce(n.rssPreview, '')) like lower(concat('%', :search, '%'))
                    or lower(coalesce(n.summary, '')) like lower(concat('%', :search, '%'))
                    or lower(coalesce(n.bodyText, '')) like lower(concat('%', :search, '%')))
            """)
    Page<Notice> search(
            @Param("category") String category,
            @Param("search") String search,
            Pageable pageable
    );

    Optional<Notice> findFirstByContentHash(String contentHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select n
            from Notice n
            where n.id = :noticeId
            """)
    Optional<Notice> findByIdForUpdate(@Param("noticeId") String noticeId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Notice n
            set n.viewCount = n.viewCount + 1
            where n.id = :noticeId
            """)
    int incrementViewCount(@Param("noticeId") String noticeId);
}
