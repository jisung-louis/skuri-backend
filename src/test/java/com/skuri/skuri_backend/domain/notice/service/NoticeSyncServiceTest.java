package com.skuri.skuri_backend.domain.notice.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeSyncResponse;
import com.skuri.skuri_backend.domain.notice.entity.Notice;
import com.skuri.skuri_backend.domain.notice.entity.NoticeCategory;
import com.skuri.skuri_backend.domain.notice.repository.NoticeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeSyncServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private NoticeRssClient noticeRssClient;

    @Mock
    private NoticeDetailCrawler noticeDetailCrawler;

    @Mock
    private NoticeSyncLock noticeSyncLock;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NoticeSyncService noticeSyncService;

    @Test
    void syncSingleNotice_다른안정ID라도_contentHash가같으면_중복생성을건너뛴다() {
        Notice existing = storedNotice("existing-id", "same-content-hash", "rss-1", "detail-1", "<p>기존</p>");
        NoticeFeedItem item = new NoticeFeedItem(
                "new-id",
                "공지 제목",
                "RSS 미리보기",
                "https://www.sungkyul.ac.kr/new-link",
                LocalDateTime.of(2026, 2, 1, 9, 0),
                "2026-02-01 09:00:00",
                "학사",
                "성결대학교",
                "교무처",
                "RSS",
                "rss-2"
        );
        String detailHash = NoticeHashUtils.detailHash("<p>동일 상세</p>", List.of());
        String contentHash = NoticeHashUtils.contentHash(item.title(), item.rssPreview(), item.category(), item.postedAt(), item.author(), detailHash);
        ReflectionTestUtils.setField(existing, "contentHash", contentHash);

        when(noticeRepository.findById("new-id")).thenReturn(Optional.empty());
        when(noticeRepository.findFirstByContentHash(contentHash)).thenReturn(Optional.of(existing));
        when(noticeDetailCrawler.crawl(item.link())).thenReturn(NoticeCrawledDetail.of("<p>동일 상세</p>", "동일 상세", List.of()));

        NoticeSyncService.SyncOutcome outcome = noticeSyncService.syncSingleNotice(item, LocalDateTime.now(), true);

        assertEquals(NoticeSyncService.SyncOutcome.SKIPPED, outcome);
        verify(noticeRepository, never()).save(any(Notice.class));
    }

    @Test
    void syncManually_같은데이터반복실행시_멱등하다() {
        AtomicReference<Notice> stored = new AtomicReference<>();
        NoticeFeedItem item = noticeFeedItem("notice-1", "rss-1");

        when(noticeSyncLock.tryLock()).thenReturn(true);
        when(noticeRssClient.fetch(any(NoticeCategory.class), eq(10))).thenAnswer(invocation -> {
            NoticeCategory category = invocation.getArgument(0);
            return category == NoticeCategory.ACADEMIC ? List.of(item) : List.of();
        });
        when(noticeDetailCrawler.crawl(item.link())).thenReturn(NoticeCrawledDetail.of("<p>상세</p>", "상세", List.of()));
        when(noticeRepository.findById("notice-1")).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(noticeRepository.findFirstByContentHash(any())).thenAnswer(invocation -> {
            Notice current = stored.get();
            if (current == null) {
                return Optional.empty();
            }
            String hash = invocation.getArgument(0);
            return hash.equals(current.getContentHash()) ? Optional.of(current) : Optional.empty();
        });
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> {
            Notice notice = invocation.getArgument(0);
            if (notice.getCreatedAt() == null) {
                ReflectionTestUtils.setField(notice, "createdAt", LocalDateTime.now());
            }
            ReflectionTestUtils.setField(notice, "updatedAt", LocalDateTime.now());
            stored.set(notice);
            return notice;
        });

        NoticeSyncResponse first = noticeSyncService.syncManually();
        NoticeSyncResponse second = noticeSyncService.syncManually();

        assertEquals(1, first.created());
        assertEquals(0, first.updated());
        assertEquals(0, first.skipped());
        assertEquals(0, first.failed());
        assertEquals(0, second.created());
        assertEquals(0, second.updated());
        assertEquals(1, second.skipped());
        assertEquals(0, second.failed());
        assertNotNull(stored.get());
    }

    @Test
    void syncSingleNotice_상세본문이바뀌면_업데이트된다() {
        Notice existing = storedNotice("notice-1", "old-content-hash", "rss-1", "detail-old", "<p>old</p>");
        NoticeFeedItem item = noticeFeedItem("notice-1", "rss-1");
        LocalDateTime syncedAt = LocalDateTime.of(2026, 3, 6, 12, 0);
        ReflectionTestUtils.setField(existing, "detailCheckedAt", syncedAt.minusDays(2));

        when(noticeRepository.findById("notice-1")).thenReturn(Optional.of(existing));
        when(noticeDetailCrawler.crawl(item.link())).thenReturn(NoticeCrawledDetail.of("<p>new</p>", "new", List.of()));
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoticeSyncService.SyncOutcome outcome = noticeSyncService.syncSingleNotice(item, syncedAt, false);

        assertEquals(NoticeSyncService.SyncOutcome.UPDATED, outcome);
        assertNotEquals("old-content-hash", existing.getContentHash());
        assertEquals("new", existing.getBodyText());
        assertEquals("<p>new</p>", existing.getBodyHtml());
    }

    @Test
    void syncSingleNotice_상세크롤링실패시_기존상세를보존한다() {
        Notice existing = storedNotice("notice-1", "old-content-hash", "rss-1", "detail-old", "<p>old</p>");
        NoticeFeedItem item = noticeFeedItem("notice-1", "rss-2");
        LocalDateTime originalCheckedAt = existing.getDetailCheckedAt();
        String originalDetailHash = existing.getDetailHash();

        when(noticeRepository.findById("notice-1")).thenReturn(Optional.of(existing));
        when(noticeDetailCrawler.crawl(item.link())).thenReturn(NoticeCrawledDetail.failed());
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoticeSyncService.SyncOutcome outcome = noticeSyncService.syncSingleNotice(item, LocalDateTime.of(2026, 3, 6, 12, 0), true);

        assertEquals(NoticeSyncService.SyncOutcome.UPDATED, outcome);
        assertEquals("상세 텍스트", existing.getBodyText());
        assertEquals("<p>old</p>", existing.getBodyHtml());
        assertEquals(originalDetailHash, existing.getDetailHash());
        assertEquals(originalCheckedAt, existing.getDetailCheckedAt());
    }

    @Test
    void syncManually_특정공지저장실패시_다음공지계속진행하고_failed로집계한다() {
        NoticeFeedItem failedItem = new NoticeFeedItem(
                "notice-failed",
                "실패 공지",
                "RSS 미리보기",
                "https://www.sungkyul.ac.kr/notice/failed",
                LocalDateTime.of(2026, 2, 1, 9, 0),
                "2026-02-01 09:00:00",
                "학사",
                "성결대학교",
                "교무처",
                "RSS",
                "rss-failed"
        );
        NoticeFeedItem successItem = new NoticeFeedItem(
                "notice-success",
                "성공 공지",
                "RSS 미리보기",
                "https://www.sungkyul.ac.kr/notice/success",
                LocalDateTime.of(2026, 2, 1, 10, 0),
                "2026-02-01 10:00:00",
                "학사",
                "성결대학교",
                "교무처",
                "RSS",
                "rss-success"
        );

        when(noticeSyncLock.tryLock()).thenReturn(true);
        when(noticeRssClient.fetch(any(NoticeCategory.class), eq(10))).thenAnswer(invocation -> {
            NoticeCategory category = invocation.getArgument(0);
            return category == NoticeCategory.ACADEMIC ? List.of(failedItem, successItem) : List.of();
        });
        when(noticeRepository.findById("notice-failed")).thenReturn(Optional.empty());
        when(noticeRepository.findById("notice-success")).thenReturn(Optional.empty());
        when(noticeRepository.findFirstByContentHash(any())).thenReturn(Optional.empty());
        when(noticeDetailCrawler.crawl(any())).thenReturn(NoticeCrawledDetail.of("<p>상세</p>", "상세", List.of()));
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> {
            Notice notice = invocation.getArgument(0);
            if ("notice-failed".equals(notice.getId())) {
                throw new DataIntegrityViolationException("content_detail too long");
            }
            return notice;
        });

        NoticeSyncResponse response = noticeSyncService.syncManually();

        assertEquals(1, response.created());
        assertEquals(0, response.updated());
        assertEquals(0, response.skipped());
        assertEquals(1, response.failed());
        verify(noticeRepository, times(2)).save(any(Notice.class));
    }

    private NoticeFeedItem noticeFeedItem(String id, String rssFingerprint) {
        return new NoticeFeedItem(
                id,
                "공지 제목",
                "RSS 미리보기",
                "https://www.sungkyul.ac.kr/notice/1",
                LocalDateTime.of(2026, 2, 1, 9, 0),
                "2026-02-01 09:00:00",
                "학사",
                "성결대학교",
                "교무처",
                "RSS",
                rssFingerprint
        );
    }

    private Notice storedNotice(String id, String contentHash, String rssFingerprint, String detailHash, String html) {
        Notice notice = Notice.create(
                id,
                "공지 제목",
                "RSS 미리보기",
                "https://www.sungkyul.ac.kr/notice/1",
                LocalDateTime.of(2026, 2, 1, 9, 0),
                "학사",
                "성결대학교",
                "교무처",
                "RSS",
                rssFingerprint,
                detailHash,
                contentHash,
                LocalDateTime.of(2026, 3, 1, 12, 0),
                "상세 텍스트",
                html,
                List.of()
        );
        ReflectionTestUtils.setField(notice, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(notice, "updatedAt", LocalDateTime.now());
        return notice;
    }
}
