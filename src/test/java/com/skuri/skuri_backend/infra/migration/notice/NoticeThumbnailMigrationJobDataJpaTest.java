package com.skuri.skuri_backend.infra.migration.notice;

import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
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
        MigrationReportWriter.class,
        NoticeThumbnailMigrationJob.class,
        NoticeThumbnailMigrationJobDataJpaTest.TestJdbcConfig.class
})
class NoticeThumbnailMigrationJobDataJpaTest {

    @Autowired
    private NoticeThumbnailMigrationJob noticeThumbnailMigrationJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @TempDir
    Path tempDir;

    @Test
    void apply_bodyHtml기준으로_thumbnailUrl을_backfill하고_재실행시_idempotent하다() throws Exception {
        insertNotice("notice-1", "<p>본문</p><img src=\"https://example.com/notice-1-thumb.jpg\" />", null);
        insertNotice("notice-2", "<p>이미지 없음</p>", "https://example.com/legacy-thumb.jpg");
        insertNotice("notice-3", "<p>본문</p><img src=\"https://example.com/notice-3-thumb.jpg\" />", "https://example.com/notice-3-thumb.jpg");

        Path reportRoot = tempDir.resolve("reports");
        MigrationExecutionResult first = noticeThumbnailMigrationJob.execute(
                new MigrationRunOptions(MigrationMode.APPLY, 2, true, reportRoot, LocalDateTime.of(2026, 4, 6, 10, 0))
        );

        assertEquals(Map.of(
                "scanned", 3L,
                "updated", 2L,
                "no_image", 1L,
                "failed", 0L
        ), first.summary().counters());
        assertTrue(Files.exists(first.reportDirectory().resolve("summary.json")));

        assertEquals("https://example.com/notice-1-thumb.jpg", thumbnailOf("notice-1"));
        assertEquals(null, thumbnailOf("notice-2"));
        assertEquals("https://example.com/notice-3-thumb.jpg", thumbnailOf("notice-3"));

        MigrationExecutionResult second = noticeThumbnailMigrationJob.execute(
                new MigrationRunOptions(MigrationMode.APPLY, 2, true, reportRoot, LocalDateTime.of(2026, 4, 6, 10, 10))
        );

        assertEquals(Map.of(
                "scanned", 3L,
                "updated", 0L,
                "no_image", 1L,
                "failed", 0L
        ), second.summary().counters());
        assertEquals("https://example.com/notice-1-thumb.jpg", thumbnailOf("notice-1"));
        assertEquals(null, thumbnailOf("notice-2"));
    }

    @Test
    void dryRun_요약만기록하고_db는변경하지않는다() throws Exception {
        insertNotice("dry-run-1", "<p>본문</p><img src=\"https://example.com/dry-run-thumb.jpg\" />", null);
        insertNotice("dry-run-2", "<p>이미지 없음</p>", "https://example.com/legacy-thumb.jpg");

        Path reportRoot = tempDir.resolve("dry-run-reports");
        MigrationExecutionResult result = noticeThumbnailMigrationJob.execute(
                new MigrationRunOptions(MigrationMode.DRY_RUN, 10, true, reportRoot, LocalDateTime.of(2026, 4, 6, 11, 0))
        );

        assertEquals(Map.of(
                "scanned", 2L,
                "updated", 2L,
                "no_image", 1L,
                "failed", 0L
        ), result.summary().counters());
        assertTrue(Files.exists(result.reportDirectory().resolve("summary.json")));
        assertEquals(null, thumbnailOf("dry-run-1"));
        assertEquals("https://example.com/legacy-thumb.jpg", thumbnailOf("dry-run-2"));
    }

    private String thumbnailOf(String noticeId) {
        return jdbcTemplate.queryForObject(
                "select thumbnail_url from notices where id = ?",
                String.class,
                noticeId
        );
    }

    private void insertNotice(String id, String bodyHtml, String thumbnailUrl) {
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
                .setParameter("id", id)
                .setParameter("title", "공지 " + id)
                .setParameter("rssPreview", "RSS " + id)
                .setParameter("summary", null)
                .setParameter("link", "https://example.com/notices/" + id)
                .setParameter("postedAt", LocalDateTime.of(2026, 4, 1, 9, 0))
                .setParameter("category", "학사")
                .setParameter("department", "교무처")
                .setParameter("author", "교무처")
                .setParameter("source", "RSS")
                .setParameter("rssFingerprint", "rss-" + id)
                .setParameter("detailHash", "detail-" + id)
                .setParameter("contentHash", "content-" + id)
                .setParameter("detailCheckedAt", LocalDateTime.of(2026, 4, 1, 10, 0))
                .setParameter("bodyText", "본문 " + id)
                .setParameter("bodyHtml", bodyHtml)
                .setParameter("thumbnailUrl", thumbnailUrl)
                .setParameter("attachments", "[]")
                .setParameter("viewCount", 0)
                .setParameter("likeCount", 0)
                .setParameter("commentCount", 0)
                .setParameter("bookmarkCount", 0)
                .setParameter("createdAt", LocalDateTime.of(2026, 4, 1, 9, 0))
                .setParameter("updatedAt", LocalDateTime.of(2026, 4, 1, 9, 30))
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
