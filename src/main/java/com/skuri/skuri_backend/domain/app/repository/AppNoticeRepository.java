package com.skuri.skuri_backend.domain.app.repository;

import com.skuri.skuri_backend.domain.app.entity.AppNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppNoticeRepository extends JpaRepository<AppNotice, String> {

    @Query("""
            select a
            from AppNotice a
            where a.publishedAt <= :now
            order by a.publishedAt desc, a.createdAt desc
            """)
    List<AppNotice> findPublished(@Param("now") LocalDateTime now);

    @Query("""
            select a
            from AppNotice a
            where a.id = :appNoticeId
              and a.publishedAt <= :now
            """)
    Optional<AppNotice> findPublishedById(@Param("appNoticeId") String appNoticeId, @Param("now") LocalDateTime now);

    @Query("""
            select count(a)
            from AppNotice a
            where a.publishedAt <= :now
              and not exists (
                    select 1
                    from AppNoticeReadStatus s
                    where s.id.userId = :userId
                      and s.id.appNoticeId = a.id
              )
            """)
    long countPublishedUnread(@Param("userId") String userId, @Param("now") LocalDateTime now);
}
