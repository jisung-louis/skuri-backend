package com.skuri.skuri_backend.infra.migration.notice;

import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.domain.notice.repository.NoticeRepository;
import com.skuri.skuri_backend.infra.migration.FirestoreTimestampParser;
import com.skuri.skuri_backend.infra.migration.MigrationExecutionResult;
import com.skuri.skuri_backend.infra.migration.MigrationMode;
import com.skuri.skuri_backend.infra.migration.MigrationReportWriter;
import com.skuri.skuri_backend.infra.migration.MigrationRunOptions;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({
        ObjectMapperConfig.class,
        FirestoreTimestampParser.class,
        MigrationReportWriter.class,
        NoticeMigrationJob.class,
        NoticeMigrationJobDataJpaTest.TestJdbcConfig.class
})
class NoticeMigrationJobDataJpaTest {

    @Autowired
    private NoticeMigrationJob noticeMigrationJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private EntityManager entityManager;

    @TempDir
    Path tempDir;

    @Test
    void apply_기존공지는카운터를보존하고_신규공지는삽입한다() throws Exception {
        insertExistingNotice();

        Path inputFile = tempDir.resolve("notices.json");
        Files.writeString(inputFile, """
                [
                  {
                    "id": "existing-notice",
                    "data": {
                      "id": "existing-notice",
                      "title": "갱신된 공지 제목",
                      "content": "갱신된 미리보기",
                      "link": "https://example.com/notices/existing",
                      "postedAt": {"_seconds": 1775000000, "_nanoseconds": 0},
                      "category": "학사",
                      "author": "교무처",
                      "department": "교무처",
                      "source": "RSS",
                      "contentHash": "legacy-existing-hash",
                      "contentDetail": "<p>갱신 본문</p>",
                      "contentAttachments": [],
                      "createdAt": {"_seconds": 1775000100, "_nanoseconds": 0},
                      "updatedAt": {"_seconds": 1775000200, "_nanoseconds": 0},
                      "viewCount": 999,
                      "likeCount": 999
                    }
                  },
                  {
                    "id": "new-notice",
                    "data": {
                      "id": "new-notice",
                      "title": "신규 공지 제목",
                      "content": "신규 미리보기",
                      "link": "https://example.com/notices/new",
                      "postedAt": {"_seconds": 1775100000, "_nanoseconds": 0},
                      "category": "장학",
                      "author": "학생처",
                      "department": "학생처",
                      "source": "RSS",
                      "contentHash": "legacy-new-hash",
                      "contentDetail": "<p>신규 본문</p><img src=\\"https://example.com/thumb.jpg\\" />",
                      "contentAttachments": [
                        {
                          "name": "file.pdf",
                          "downloadUrl": "https://example.com/file.pdf",
                          "previewUrl": "https://example.com/file-preview"
                        }
                      ],
                      "createdAt": {"_seconds": 1775100100, "_nanoseconds": 0},
                      "updatedAt": {"_seconds": 1775100200, "_nanoseconds": 0},
                      "viewCount": 7,
                      "likeCount": 5
                    }
                  }
                ]
                """);

        Path reportRoot = tempDir.resolve("reports");
        MigrationExecutionResult result = noticeMigrationJob.execute(
                inputFile,
                new MigrationRunOptions(MigrationMode.APPLY, 100, true, reportRoot, LocalDateTime.of(2026, 4, 2, 23, 30))
        );

        assertEquals(Map.of(
                "scanned", 2L,
                "inserted", 1L,
                "updated", 1L,
                "rejected", 0L
        ), result.summary().counters());
        assertTrue(Files.exists(result.reportDirectory().resolve("summary.json")));
        assertTrue(Files.exists(result.reportDirectory().resolve("rejects.json")));

        Map<String, Object> existing = jdbcTemplate.queryForMap(
                "select title, rss_preview, body_text, view_count, like_count, comment_count, bookmark_count from notices where id = ?",
                "existing-notice"
        );
        assertEquals("갱신된 공지 제목", existing.get("title"));
        assertEquals("갱신된 미리보기", existing.get("rss_preview"));
        assertEquals("갱신 본문", existing.get("body_text"));
        assertEquals(11, ((Number) existing.get("view_count")).intValue());
        assertEquals(12, ((Number) existing.get("like_count")).intValue());
        assertEquals(13, ((Number) existing.get("comment_count")).intValue());
        assertEquals(14, ((Number) existing.get("bookmark_count")).intValue());

        Map<String, Object> created = jdbcTemplate.queryForMap(
                "select title, rss_fingerprint, body_text, thumbnail_url, view_count, like_count, attachments from notices where id = ?",
                "new-notice"
        );
        assertEquals("신규 공지 제목", created.get("title"));
        assertEquals("legacy-new-hash", created.get("rss_fingerprint"));
        assertEquals("신규 본문", created.get("body_text"));
        assertEquals("https://example.com/thumb.jpg", created.get("thumbnail_url"));
        assertEquals(7, ((Number) created.get("view_count")).intValue());
        assertEquals(5, ((Number) created.get("like_count")).intValue());
        assertEquals("file.pdf", noticeRepository.findById("new-notice").orElseThrow().getAttachments().getFirst().name());
    }

    @Test
    void dryRun_요약만생성하고_db는변경하지않는다() throws Exception {
        Path inputFile = tempDir.resolve("notices-dry-run.json");
        Files.writeString(inputFile, """
                [
                  {
                    "id": "dry-run-notice",
                    "data": {
                      "id": "dry-run-notice",
                      "title": "드라이런 공지",
                      "content": "미리보기",
                      "link": "https://example.com/notices/dry-run",
                      "postedAt": {"_seconds": 1775200000, "_nanoseconds": 0},
                      "category": "학사",
                      "author": "교무처",
                      "department": "교무처",
                      "source": "RSS",
                      "contentHash": "legacy-dry-run-hash",
                      "contentDetail": "<p>본문</p>",
                      "contentAttachments": [],
                      "createdAt": {"_seconds": 1775200100, "_nanoseconds": 0},
                      "updatedAt": {"_seconds": 1775200200, "_nanoseconds": 0},
                      "viewCount": 1,
                      "likeCount": 2
                    }
                  }
                ]
                """);

        Path reportRoot = tempDir.resolve("dry-run-reports");
        MigrationExecutionResult result = noticeMigrationJob.execute(
                inputFile,
                new MigrationRunOptions(MigrationMode.DRY_RUN, 50, true, reportRoot, LocalDateTime.of(2026, 4, 2, 23, 40))
        );

        Integer count = jdbcTemplate.queryForObject("select count(*) from notices where id = ?", Integer.class, "dry-run-notice");
        assertEquals(0, count);
        assertEquals(Map.of(
                "scanned", 1L,
                "inserted", 1L,
                "updated", 0L,
                "rejected", 0L
        ), result.summary().counters());
        assertTrue(Files.exists(result.reportDirectory().resolve("summary.json")));
    }

    private void insertExistingNotice() {
        entityManager.createNativeQuery("""
                insert into notices (
                    id, title, rss_preview, summary, link, posted_at, category, department, author, source,
                    rss_fingerprint, detail_hash, content_hash, detail_checked_at, body_text, body_html, thumbnail_url, attachments,
                    view_count, like_count, comment_count, bookmark_count, created_at, updated_at
                ) values (
                    :id, :title, :rssPreview, :summary, :link, :postedAt, :category, :department, :author, :source,
                    :rssFingerprint, :detailHash, :contentHash, :detailCheckedAt, :bodyText, :bodyHtml, :thumbnailUrl, :attachments,
                    :viewCount, :likeCount, :commentCount, :bookmarkCount, :createdAt, :updatedAt
                )
                """)
                .setParameter("id", "existing-notice")
                .setParameter("title", "기존 제목")
                .setParameter("rssPreview", "기존 미리보기")
                .setParameter("summary", null)
                .setParameter("link", "https://example.com/notices/existing")
                .setParameter("postedAt", LocalDateTime.of(2026, 3, 1, 9, 0))
                .setParameter("category", "학사")
                .setParameter("department", "교무처")
                .setParameter("author", "교무처")
                .setParameter("source", "RSS")
                .setParameter("rssFingerprint", "old-rss")
                .setParameter("detailHash", "old-detail")
                .setParameter("contentHash", "old-content")
                .setParameter("detailCheckedAt", LocalDateTime.of(2026, 3, 1, 12, 0))
                .setParameter("bodyText", "기존 본문")
                .setParameter("bodyHtml", "<p>기존 본문</p>")
                .setParameter("thumbnailUrl", null)
                .setParameter("attachments", "[]")
                .setParameter("viewCount", 11)
                .setParameter("likeCount", 12)
                .setParameter("commentCount", 13)
                .setParameter("bookmarkCount", 14)
                .setParameter("createdAt", LocalDateTime.of(2026, 3, 1, 9, 0))
                .setParameter("updatedAt", LocalDateTime.of(2026, 3, 1, 10, 0))
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    static class TestJdbcConfig {

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
