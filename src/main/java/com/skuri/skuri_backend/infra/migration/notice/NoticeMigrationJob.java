package com.skuri.skuri_backend.infra.migration.notice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.domain.notice.entity.Notice;
import com.skuri.skuri_backend.domain.notice.entity.NoticeAttachment;
import com.skuri.skuri_backend.domain.notice.repository.NoticeRepository;
import com.skuri.skuri_backend.domain.notice.service.NoticeBodyTextExtractor;
import com.skuri.skuri_backend.domain.notice.service.NoticeHashUtils;
import com.skuri.skuri_backend.infra.migration.FileHashUtils;
import com.skuri.skuri_backend.infra.migration.FirestoreTimestampParser;
import com.skuri.skuri_backend.infra.migration.JsonArrayFileReader;
import com.skuri.skuri_backend.infra.migration.MigrationExecutionResult;
import com.skuri.skuri_backend.infra.migration.MigrationMode;
import com.skuri.skuri_backend.infra.migration.MigrationReject;
import com.skuri.skuri_backend.infra.migration.MigrationReportWriter;
import com.skuri.skuri_backend.infra.migration.MigrationRunOptions;
import com.skuri.skuri_backend.infra.migration.MigrationSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class NoticeMigrationJob {

    private static final String INSERT_SQL = """
            insert into notices (
                id, title, rss_preview, summary, link, posted_at, category, department, author, source,
                rss_fingerprint, detail_hash, content_hash, detail_checked_at, body_text, body_html, attachments,
                view_count, like_count, comment_count, bookmark_count, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            update notices
            set title = ?,
                rss_preview = ?,
                summary = ?,
                link = ?,
                posted_at = ?,
                category = ?,
                department = ?,
                author = ?,
                source = ?,
                rss_fingerprint = ?,
                detail_hash = ?,
                content_hash = ?,
                detail_checked_at = ?,
                body_text = ?,
                body_html = ?,
                attachments = ?,
                updated_at = ?
            where id = ?
            """;

    private final NoticeRepository noticeRepository;
    private final JdbcTemplate jdbcTemplate;
    private final FirestoreTimestampParser timestampParser;
    private final ObjectMapper objectMapper;
    private final MigrationReportWriter reportWriter;

    public NoticeMigrationJob(
            NoticeRepository noticeRepository,
            JdbcTemplate jdbcTemplate,
            FirestoreTimestampParser timestampParser,
            ObjectMapper objectMapper,
            MigrationReportWriter reportWriter
    ) {
        this.noticeRepository = noticeRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.timestampParser = timestampParser;
        this.objectMapper = objectMapper;
        this.reportWriter = reportWriter;
    }

    @Transactional
    public MigrationExecutionResult execute(Path noticeFile, MigrationRunOptions options) {
        AtomicLong scannedCount = new AtomicLong();
        AtomicLong insertedCount = new AtomicLong();
        AtomicLong updatedCount = new AtomicLong();
        List<MigrationReject> rejects = new ArrayList<>();
        List<NoticeUpsertRow> buffer = new ArrayList<>(options.batchSize());
        Set<String> seenIds = new HashSet<>();

        JsonArrayFileReader.read(noticeFile, objectMapper, NoticeExportItem.class, item -> {
            scannedCount.incrementAndGet();
            NoticeUpsertRow row;
            try {
                row = mapRow(item, options.startedAt(), seenIds);
            } catch (IllegalArgumentException e) {
                rejects.add(reject(item, e.getMessage()));
                return;
            }

            buffer.add(row);
            if (buffer.size() >= options.batchSize()) {
                flush(buffer, options.mode(), insertedCount, updatedCount);
                buffer.clear();
            }
        });

        if (!buffer.isEmpty()) {
            flush(buffer, options.mode(), insertedCount, updatedCount);
            buffer.clear();
        }

        MigrationSummary summary = new MigrationSummary(
                "NOTICES",
                options.mode().name(),
                noticeFile.toString(),
                FileHashUtils.sha256(noticeFile),
                options.startedAt(),
                LocalDateTime.now(),
                Map.of(
                        "scanned", scannedCount.get(),
                        "inserted", insertedCount.get(),
                        "updated", updatedCount.get(),
                        "rejected", (long) rejects.size()
                )
        );
        Path reportDirectory = reportWriter.write("notices", summary, rejects, options.reportRootDirectory());

        if (!rejects.isEmpty() && options.failOnReject()) {
            throw new IllegalStateException("공지 마이그레이션 reject가 발생했습니다. reportDirectory=" + reportDirectory);
        }

        return new MigrationExecutionResult(summary, reportDirectory, List.copyOf(rejects));
    }

    private void flush(
            List<NoticeUpsertRow> rows,
            MigrationMode mode,
            AtomicLong insertedCount,
            AtomicLong updatedCount
    ) {
        Set<String> existingIds = findExistingIds(rows);
        List<NoticeUpsertRow> inserts = new ArrayList<>();
        List<NoticeUpsertRow> updates = new ArrayList<>();

        for (NoticeUpsertRow row : rows) {
            if (existingIds.contains(row.id())) {
                updates.add(row);
            } else {
                inserts.add(row);
            }
        }

        insertedCount.addAndGet(inserts.size());
        updatedCount.addAndGet(updates.size());

        if (mode == MigrationMode.DRY_RUN) {
            return;
        }

        if (!inserts.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_SQL, new NoticeInsertSetter(inserts));
        }
        if (!updates.isEmpty()) {
            jdbcTemplate.batchUpdate(UPDATE_SQL, new NoticeUpdateSetter(updates));
        }
    }

    private Set<String> findExistingIds(List<NoticeUpsertRow> rows) {
        Set<String> requestedIds = new LinkedHashSet<>();
        for (NoticeUpsertRow row : rows) {
            requestedIds.add(row.id());
        }
        Set<String> existingIds = new HashSet<>();
        for (Notice notice : noticeRepository.findAllById(requestedIds)) {
            existingIds.add(notice.getId());
        }
        return existingIds;
    }

    private NoticeUpsertRow mapRow(NoticeExportItem item, LocalDateTime importStartedAt, Set<String> seenIds) {
        NoticeExportItem.NoticeExportData data = item.data();
        if (data == null) {
            throw new IllegalArgumentException("data가 비어 있습니다.");
        }

        String id = firstNonBlank(item.id(), data.id());
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("공지 id가 비어 있습니다.");
        }
        if (!seenIds.add(id)) {
            throw new IllegalArgumentException("중복 공지 id입니다: " + id);
        }

        String title = requireText(data.title(), "title");
        String link = requireText(data.link(), "link");
        String rssFingerprint = requireText(data.contentHash(), "contentHash");
        String rssPreview = trimToNull(data.content());
        String category = trimToNull(data.category());
        String department = trimToNull(data.department());
        String author = trimToNull(data.author());
        String source = trimToNull(data.source());
        String bodyHtml = defaultString(data.contentDetail());
        List<NoticeAttachment> attachments = data.contentAttachments() == null ? List.of() : List.copyOf(data.contentAttachments());
        String bodyText = NoticeBodyTextExtractor.extract(bodyHtml);
        String detailHash = NoticeHashUtils.detailHash(bodyHtml, attachments);
        LocalDateTime postedAt = timestampParser.toLocalDateTime(data.postedAt());
        String contentHash = NoticeHashUtils.contentHash(title, rssPreview, category, postedAt, author, detailHash);
        LocalDateTime createdAt = coalesce(timestampParser.toLocalDateTime(data.createdAt()), postedAt, importStartedAt);
        LocalDateTime updatedAt = coalesce(timestampParser.toLocalDateTime(data.updatedAt()), createdAt);

        return new NoticeUpsertRow(
                id,
                title,
                rssPreview,
                link,
                postedAt,
                category,
                department,
                author,
                source,
                rssFingerprint,
                detailHash,
                contentHash,
                importStartedAt,
                bodyText,
                bodyHtml,
                attachments,
                attachmentsJson(attachments),
                data.viewCount() == null ? 0 : data.viewCount(),
                data.likeCount() == null ? 0 : data.likeCount(),
                createdAt,
                updatedAt
        );
    }

    private MigrationReject reject(NoticeExportItem item, String reason) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("inputId", item.id());
        if (item.data() != null) {
            detail.put("dataId", item.data().id());
            detail.put("title", item.data().title());
            detail.put("link", item.data().link());
        }
        return new MigrationReject(firstNonBlank(item.id(), item.data() == null ? null : item.data().id()), reason, detail);
    }

    private String attachmentsJson(List<NoticeAttachment> attachments) {
        try {
            return objectMapper.writeValueAsString(attachments);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("공지 첨부파일 JSON 직렬화에 실패했습니다.", e);
        }
    }

    private LocalDateTime coalesce(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " 값이 비어 있습니다.");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary.trim() : trimToNull(fallback);
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private record NoticeUpsertRow(
            String id,
            String title,
            String rssPreview,
            String link,
            LocalDateTime postedAt,
            String category,
            String department,
            String author,
            String source,
            String rssFingerprint,
            String detailHash,
            String contentHash,
            LocalDateTime detailCheckedAt,
            String bodyText,
            String bodyHtml,
            List<NoticeAttachment> attachments,
            String attachmentsJson,
            int viewCount,
            int likeCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    private static final class NoticeInsertSetter implements BatchPreparedStatementSetter {

        private final List<NoticeUpsertRow> rows;

        private NoticeInsertSetter(List<NoticeUpsertRow> rows) {
            this.rows = rows;
        }

        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            NoticeUpsertRow row = rows.get(i);
            ps.setString(1, row.id());
            ps.setString(2, row.title());
            ps.setString(3, row.rssPreview());
            ps.setString(4, null);
            ps.setString(5, row.link());
            ps.setTimestamp(6, toTimestamp(row.postedAt()));
            ps.setString(7, row.category());
            ps.setString(8, row.department());
            ps.setString(9, row.author());
            ps.setString(10, row.source());
            ps.setString(11, row.rssFingerprint());
            ps.setString(12, row.detailHash());
            ps.setString(13, row.contentHash());
            ps.setTimestamp(14, toTimestamp(row.detailCheckedAt()));
            ps.setString(15, row.bodyText());
            ps.setString(16, row.bodyHtml());
            ps.setString(17, row.attachmentsJson());
            ps.setInt(18, row.viewCount());
            ps.setInt(19, row.likeCount());
            ps.setInt(20, 0);
            ps.setInt(21, 0);
            ps.setTimestamp(22, toTimestamp(row.createdAt()));
            ps.setTimestamp(23, toTimestamp(row.updatedAt()));
        }

        @Override
        public int getBatchSize() {
            return rows.size();
        }
    }

    private static final class NoticeUpdateSetter implements BatchPreparedStatementSetter {

        private final List<NoticeUpsertRow> rows;

        private NoticeUpdateSetter(List<NoticeUpsertRow> rows) {
            this.rows = rows;
        }

        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            NoticeUpsertRow row = rows.get(i);
            ps.setString(1, row.title());
            ps.setString(2, row.rssPreview());
            ps.setString(3, null);
            ps.setString(4, row.link());
            ps.setTimestamp(5, toTimestamp(row.postedAt()));
            ps.setString(6, row.category());
            ps.setString(7, row.department());
            ps.setString(8, row.author());
            ps.setString(9, row.source());
            ps.setString(10, row.rssFingerprint());
            ps.setString(11, row.detailHash());
            ps.setString(12, row.contentHash());
            ps.setTimestamp(13, toTimestamp(row.detailCheckedAt()));
            ps.setString(14, row.bodyText());
            ps.setString(15, row.bodyHtml());
            ps.setString(16, row.attachmentsJson());
            ps.setTimestamp(17, toTimestamp(row.updatedAt()));
            ps.setString(18, row.id());
        }

        @Override
        public int getBatchSize() {
            return rows.size();
        }
    }
}
