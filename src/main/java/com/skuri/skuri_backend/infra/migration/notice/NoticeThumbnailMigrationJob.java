package com.skuri.skuri_backend.infra.migration.notice;

import com.skuri.skuri_backend.domain.notice.service.NoticeThumbnailExtractor;
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

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class NoticeThumbnailMigrationJob {

    private static final String SELECT_FIRST_BATCH_SQL = """
            select id, body_html, thumbnail_url
            from notices
            order by id asc
            limit ?
            """;

    private static final String SELECT_NEXT_BATCH_SQL = """
            select id, body_html, thumbnail_url
            from notices
            where id > ?
            order by id asc
            limit ?
            """;

    private static final String UPDATE_SQL = """
            update notices
            set thumbnail_url = ?
            where id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final MigrationReportWriter reportWriter;

    public NoticeThumbnailMigrationJob(
            JdbcTemplate jdbcTemplate,
            MigrationReportWriter reportWriter
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.reportWriter = reportWriter;
    }

    @Transactional
    public MigrationExecutionResult execute(MigrationRunOptions options) {
        AtomicLong scannedCount = new AtomicLong();
        AtomicLong updatedCount = new AtomicLong();
        AtomicLong noImageCount = new AtomicLong();
        AtomicLong failedCount = new AtomicLong();
        List<MigrationReject> rejects = new ArrayList<>();

        String lastId = null;
        while (true) {
            List<ThumbnailSourceRow> batch = fetchBatch(lastId, options.batchSize());
            if (batch.isEmpty()) {
                break;
            }

            List<ThumbnailUpdateRow> updates = new ArrayList<>();
            for (ThumbnailSourceRow row : batch) {
                scannedCount.incrementAndGet();
                try {
                    String extractedThumbnailUrl = NoticeThumbnailExtractor.extract(row.bodyHtml());
                    if (extractedThumbnailUrl == null) {
                        noImageCount.incrementAndGet();
                    }
                    if (!Objects.equals(extractedThumbnailUrl, row.thumbnailUrl())) {
                        updatedCount.incrementAndGet();
                        updates.add(new ThumbnailUpdateRow(row.id(), extractedThumbnailUrl));
                    }
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                    rejects.add(reject(row.id(), e));
                }
            }

            if (options.mode() == MigrationMode.APPLY && !updates.isEmpty()) {
                jdbcTemplate.batchUpdate(UPDATE_SQL, new NoticeThumbnailUpdateSetter(updates));
            }

            lastId = batch.get(batch.size() - 1).id();
            log.info(
                    "공지 썸네일 backfill batch 처리: lastId={}, scanned={}, pendingUpdates={}, mode={}",
                    lastId,
                    batch.size(),
                    updates.size(),
                    options.mode()
            );
        }

        MigrationSummary summary = new MigrationSummary(
                "NOTICE_THUMBNAILS",
                options.mode().name(),
                null,
                null,
                options.startedAt(),
                LocalDateTime.now(),
                Map.of(
                        "scanned", scannedCount.get(),
                        "updated", updatedCount.get(),
                        "no_image", noImageCount.get(),
                        "failed", failedCount.get()
                )
        );
        Path reportDirectory = reportWriter.write("notice-thumbnails", summary, rejects, options.reportRootDirectory());

        if (!rejects.isEmpty() && options.failOnReject()) {
            throw new IllegalStateException("공지 썸네일 backfill reject가 발생했습니다. reportDirectory=" + reportDirectory);
        }

        return new MigrationExecutionResult(summary, reportDirectory, List.copyOf(rejects));
    }

    private List<ThumbnailSourceRow> fetchBatch(String lastId, int batchSize) {
        if (lastId == null) {
            return jdbcTemplate.query(
                    SELECT_FIRST_BATCH_SQL,
                    (rs, rowNum) -> new ThumbnailSourceRow(
                            rs.getString("id"),
                            rs.getString("body_html"),
                            rs.getString("thumbnail_url")
                    ),
                    batchSize
            );
        }
        return jdbcTemplate.query(
                SELECT_NEXT_BATCH_SQL,
                (rs, rowNum) -> new ThumbnailSourceRow(
                        rs.getString("id"),
                        rs.getString("body_html"),
                        rs.getString("thumbnail_url")
                ),
                lastId,
                batchSize
        );
    }

    private MigrationReject reject(String noticeId, Exception e) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("noticeId", noticeId);
        detail.put("message", e.getMessage());
        return new MigrationReject(noticeId, "공지 썸네일 추출에 실패했습니다.", detail);
    }

    private record ThumbnailSourceRow(
            String id,
            String bodyHtml,
            String thumbnailUrl
    ) {
    }

    private record ThumbnailUpdateRow(
            String id,
            String thumbnailUrl
    ) {
    }

    private static final class NoticeThumbnailUpdateSetter implements BatchPreparedStatementSetter {

        private final List<ThumbnailUpdateRow> rows;

        private NoticeThumbnailUpdateSetter(List<ThumbnailUpdateRow> rows) {
            this.rows = rows;
        }

        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            ThumbnailUpdateRow row = rows.get(i);
            if (row.thumbnailUrl() == null) {
                ps.setNull(1, Types.VARCHAR);
            } else {
                ps.setString(1, row.thumbnailUrl());
            }
            ps.setString(2, row.id());
        }

        @Override
        public int getBatchSize() {
            return rows.size();
        }
    }
}
