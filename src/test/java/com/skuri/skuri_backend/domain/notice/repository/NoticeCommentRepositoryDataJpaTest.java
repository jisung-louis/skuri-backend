package com.skuri.skuri_backend.domain.notice.repository;

import com.skuri.skuri_backend.domain.notice.entity.Notice;
import com.skuri.skuri_backend.domain.notice.entity.NoticeComment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class NoticeCommentRepositoryDataJpaTest {

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private NoticeCommentRepository noticeCommentRepository;

    @Test
    void findCommentedNoticeIds_삭제되지않은내댓글과대댓글이있는공지만반환한다() {
        Notice directCommented = noticeRepository.save(notice("notice-1"));
        Notice repliedNotice = noticeRepository.save(notice("notice-2"));
        Notice deletedMine = noticeRepository.save(notice("notice-3"));
        Notice othersOnly = noticeRepository.save(notice("notice-4"));

        noticeCommentRepository.save(comment(directCommented, null, "member-1"));
        NoticeComment parent = noticeCommentRepository.save(comment(repliedNotice, null, "member-2"));
        noticeCommentRepository.save(comment(repliedNotice, parent, "member-1"));
        NoticeComment deletedComment = noticeCommentRepository.save(comment(deletedMine, null, "member-1"));
        deletedComment.softDelete();
        noticeCommentRepository.save(comment(othersOnly, null, "member-2"));
        noticeCommentRepository.flush();

        List<String> result = noticeCommentRepository.findCommentedNoticeIds(
                "member-1",
                List.of(directCommented.getId(), repliedNotice.getId(), deletedMine.getId(), othersOnly.getId())
        );

        assertEquals(Set.of(directCommented.getId(), repliedNotice.getId()), Set.copyOf(result));
    }

    private Notice notice(String id) {
        return Notice.create(
                id,
                "공지 " + id,
                "RSS 미리보기 " + id,
                "https://www.sungkyul.ac.kr/notice/" + id,
                LocalDateTime.of(2026, 3, 25, 12, 0),
                "학사",
                "성결대학교",
                "교무처",
                "RSS",
                "rss-hash-" + id,
                "detail-hash-" + id,
                "content-hash-" + id,
                LocalDateTime.of(2026, 3, 25, 12, 0),
                "본문 " + id,
                "<p>본문 " + id + "</p>",
                List.of()
        );
    }

    private NoticeComment comment(Notice notice, NoticeComment parent, String userId) {
        return NoticeComment.create(
                notice,
                userId,
                "작성자",
                "댓글",
                false,
                null,
                null,
                parent
        );
    }
}
