package com.skuri.skuri_backend.domain.notice.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeSyncResponse;
import com.skuri.skuri_backend.domain.notice.entity.Notice;
import com.skuri.skuri_backend.domain.notice.entity.NoticeAttachment;
import com.skuri.skuri_backend.domain.notice.entity.NoticeCategory;
import com.skuri.skuri_backend.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeSyncService {

    private static final int ROW_COUNT = 10;
    private static final long DETAIL_REVALIDATION_HOURS = 24L;

    private final NoticeRepository noticeRepository;
    private final NoticeRssClient noticeRssClient;
    private final NoticeDetailCrawler noticeDetailCrawler;
    private final NoticeSyncLock noticeSyncLock;

    public NoticeSyncResponse syncManually() {
        if (!noticeSyncLock.tryLock()) {
            throw new BusinessException(ErrorCode.RESOURCE_CONCURRENT_MODIFICATION, "공지 동기화가 이미 진행 중입니다.");
        }
        try {
            return executeSync(true);
        } finally {
            noticeSyncLock.unlock();
        }
    }

    public void syncScheduled() {
        if (!noticeSyncLock.tryLock()) {
            log.info("공지 스케줄 동기화 건너뜀: 이전 작업이 아직 진행 중입니다.");
            return;
        }
        try {
            NoticeSyncResponse response = executeSync(false);
            log.info(
                    "공지 스케줄 동기화 완료: created={}, updated={}, skipped={}, failed={}, syncedAt={}",
                    response.created(),
                    response.updated(),
                    response.skipped(),
                    response.failed(),
                    response.syncedAt()
            );
        } finally {
            noticeSyncLock.unlock();
        }
    }

    private NoticeSyncResponse executeSync(boolean forceDetailRefresh) {
        int created = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;
        LocalDateTime now = LocalDateTime.now();

        for (NoticeCategory category : NoticeCategory.values()) {
            List<NoticeFeedItem> items = noticeRssClient.fetch(category, ROW_COUNT);
            for (NoticeFeedItem item : items) {
                SyncOutcome outcome = syncSingleNotice(item, now, forceDetailRefresh);
                switch (outcome) {
                    case CREATED -> created++;
                    case UPDATED -> updated++;
                    case SKIPPED -> skipped++;
                    case FAILED -> failed++;
                }
            }
        }

        return new NoticeSyncResponse(created, updated, skipped, failed, now);
    }

    protected SyncOutcome syncSingleNotice(NoticeFeedItem item, LocalDateTime syncedAt, boolean forceDetailRefresh) {
        String bodyText = "";
        String bodyHtml = "";
        try {
            Notice existing = noticeRepository.findById(item.id()).orElse(null);
            boolean shouldCrawlDetail = forceDetailRefresh || shouldCrawlDetail(existing, item.rssFingerprint(), syncedAt);

            String detailHash = existing == null ? null : existing.getDetailHash();
            bodyText = existing == null ? "" : existing.getBodyText();
            bodyHtml = existing == null ? "" : existing.getBodyHtml();
            List<NoticeAttachment> attachments = existing == null ? List.of() : existing.getAttachments();

            if (shouldCrawlDetail) {
                NoticeCrawledDetail detail = noticeDetailCrawler.crawl(item.link());
                bodyText = detail.text();
                bodyHtml = detail.html();
                attachments = detail.attachments();
                detailHash = NoticeHashUtils.detailHash(bodyHtml, attachments);
            }

            String finalContentHash = NoticeHashUtils.contentHash(
                    item.title(),
                    item.rssPreview(),
                    item.category(),
                    item.postedAt(),
                    item.author(),
                    detailHash == null ? "" : detailHash
            );
            boolean metadataChanged = existing != null && isMetadataChanged(existing, item);

            if (existing == null) {
                Notice duplicate = noticeRepository.findFirstByContentHash(finalContentHash).orElse(null);
                if (duplicate != null) {
                    return SyncOutcome.SKIPPED;
                }
                noticeRepository.save(Notice.create(
                        item.id(),
                        item.title(),
                        item.rssPreview(),
                        item.link(),
                        item.postedAt(),
                        item.category(),
                        item.department(),
                        item.author(),
                        item.source(),
                        item.rssFingerprint(),
                        detailHash,
                        finalContentHash,
                        shouldCrawlDetail ? syncedAt : null,
                        bodyText,
                        bodyHtml,
                        attachments
                ));
                return SyncOutcome.CREATED;
            }

            if (finalContentHash.equals(existing.getContentHash())) {
                if (metadataChanged || shouldCrawlDetail) {
                    existing.applySync(
                            item.title(),
                            item.rssPreview(),
                            item.link(),
                            item.postedAt(),
                            item.category(),
                            item.department(),
                            item.author(),
                            item.source(),
                            item.rssFingerprint(),
                            detailHash,
                            finalContentHash,
                            shouldCrawlDetail ? syncedAt : existing.getDetailCheckedAt(),
                            bodyText,
                            bodyHtml,
                            attachments
                    );
                    noticeRepository.save(existing);
                    return metadataChanged ? SyncOutcome.UPDATED : SyncOutcome.SKIPPED;
                }
                return SyncOutcome.SKIPPED;
            }

            existing.clearSummary();
            existing.applySync(
                    item.title(),
                    item.rssPreview(),
                    item.link(),
                    item.postedAt(),
                    item.category(),
                    item.department(),
                    item.author(),
                    item.source(),
                    item.rssFingerprint(),
                    detailHash,
                    finalContentHash,
                    shouldCrawlDetail ? syncedAt : existing.getDetailCheckedAt(),
                    bodyText,
                    bodyHtml,
                    attachments
            );
            noticeRepository.save(existing);
            return SyncOutcome.UPDATED;
        } catch (Exception e) {
            log.warn(
                    "공지 동기화 실패: id={}, title={}, link={}, htmlBytes={}",
                    item.id(),
                    item.title(),
                    item.link(),
                    utf8Length(bodyHtml),
                    e
            );
            return SyncOutcome.FAILED;
        }
    }

    private int utf8Length(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }

    private boolean shouldCrawlDetail(Notice existing, String rssFingerprint, LocalDateTime syncedAt) {
        if (existing == null) {
            return true;
        }
        if (!rssFingerprint.equals(existing.getRssFingerprint())) {
            return true;
        }
        if (existing.getDetailHash() == null) {
            return true;
        }
        if (existing.getDetailCheckedAt() == null) {
            return true;
        }
        return existing.getDetailCheckedAt().plusHours(DETAIL_REVALIDATION_HOURS).isBefore(syncedAt);
    }

    private boolean isMetadataChanged(Notice existing, NoticeFeedItem item) {
        return !safeEquals(existing.getRssFingerprint(), item.rssFingerprint())
                || !safeEquals(existing.getTitle(), item.title())
                || !safeEquals(existing.getRssPreview(), item.rssPreview())
                || !safeEquals(existing.getLink(), item.link())
                || !safeEquals(existing.getCategory(), item.category())
                || !safeEquals(existing.getDepartment(), item.department())
                || !safeEquals(existing.getAuthor(), item.author())
                || !safeEquals(existing.getSource(), item.source())
                || !safeEquals(existing.getPostedAt(), item.postedAt());
    }

    private boolean safeEquals(Object left, Object right) {
        return java.util.Objects.equals(left, right);
    }

    enum SyncOutcome {
        CREATED,
        UPDATED,
        SKIPPED,
        FAILED
    }
}
