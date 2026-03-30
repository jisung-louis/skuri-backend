package com.skuri.skuri_backend.domain.notice.repository;

import com.skuri.skuri_backend.domain.notice.entity.Notice;
import com.skuri.skuri_backend.domain.notice.entity.NoticeBookmark;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class NoticeBookmarkRepositoryDataJpaTest {

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private NoticeBookmarkRepository noticeBookmarkRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findBookmarkedNotices_북마크한공지먼조회하고_postedAt_createdAt정렬을적용한다() {
        String userId = "member-1";
        LocalDateTime now = LocalDateTime.of(2026, 3, 25, 12, 0);

        Notice latestPosted = noticeRepository.save(notice("notice-1", now.plusDays(1)));
        Notice olderCreatedTie = noticeRepository.save(notice("notice-2", now));
        Notice newerCreatedTie = noticeRepository.save(notice("notice-3", now));
        Notice notBookmarked = noticeRepository.save(notice("notice-4", now.plusDays(2)));
        noticeRepository.flush();

        updateCreatedAt(latestPosted.getId(), now.minusDays(3));
        updateCreatedAt(olderCreatedTie.getId(), now.minusDays(2));
        updateCreatedAt(newerCreatedTie.getId(), now.minusDays(1));
        updateCreatedAt(notBookmarked.getId(), now);

        noticeBookmarkRepository.save(NoticeBookmark.create(latestPosted, userId));
        noticeBookmarkRepository.save(NoticeBookmark.create(olderCreatedTie, userId));
        noticeBookmarkRepository.save(NoticeBookmark.create(newerCreatedTie, userId));
        noticeBookmarkRepository.save(NoticeBookmark.create(notBookmarked, "member-2"));
        noticeBookmarkRepository.flush();
        entityManager.clear();

        Page<Notice> result = noticeBookmarkRepository.findBookmarkedNotices(
                userId,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "postedAt", "createdAt"))
        );

        assertEquals(3, result.getTotalElements());
        assertEquals(
                List.of("notice-1", "notice-3", "notice-2"),
                result.getContent().stream().map(Notice::getId).toList()
        );
    }

    @Test
    void findBookmarkedNotices_페이징이동작한다() {
        String userId = "member-1";
        LocalDateTime now = LocalDateTime.of(2026, 3, 25, 12, 0);

        Notice first = noticeRepository.save(notice("notice-1", now.plusDays(3)));
        Notice second = noticeRepository.save(notice("notice-2", now.plusDays(2)));
        Notice third = noticeRepository.save(notice("notice-3", now.plusDays(1)));
        noticeRepository.flush();

        noticeBookmarkRepository.save(NoticeBookmark.create(first, userId));
        noticeBookmarkRepository.save(NoticeBookmark.create(second, userId));
        noticeBookmarkRepository.save(NoticeBookmark.create(third, userId));
        noticeBookmarkRepository.flush();
        entityManager.clear();

        PageRequest pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "postedAt", "createdAt"));

        Page<Notice> result = noticeBookmarkRepository.findBookmarkedNotices(userId, pageable);

        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertEquals(List.of("notice-3"), result.getContent().stream().map(Notice::getId).toList());
    }

    private Notice notice(String id, LocalDateTime postedAt) {
        return Notice.create(
                id,
                "공지 " + id,
                "RSS 미리보기 " + id,
                "https://www.sungkyul.ac.kr/notice/" + id,
                postedAt,
                "학사",
                "성결대학교",
                "교무처",
                "RSS",
                "rss-hash-" + id,
                "detail-hash-" + id,
                "content-hash-" + id,
                postedAt,
                "본문 " + id,
                "<p>본문 " + id + "</p>",
                List.of()
        );
    }

    private void updateCreatedAt(String noticeId, LocalDateTime createdAt) {
        entityManager.createNativeQuery("""
                update notices
                set created_at = :createdAt,
                    updated_at = :createdAt
                where id = :id
                """)
                .setParameter("createdAt", createdAt)
                .setParameter("id", noticeId)
                .executeUpdate();
    }
}
