package com.skuri.skuri_backend.domain.admin.dashboard.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardActivityResponse;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardRecentItemResponse;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardSummaryResponse;
import com.skuri.skuri_backend.domain.app.entity.AppNotice;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeCategory;
import com.skuri.skuri_backend.domain.app.entity.AppNoticePriority;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeRepository;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.entity.Inquiry;
import com.skuri.skuri_backend.domain.support.entity.InquiryType;
import com.skuri.skuri_backend.domain.support.entity.Report;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.repository.InquiryRepository;
import com.skuri.skuri_backend.domain.support.repository.ReportRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private AppNoticeRepository appNoticeRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    void getSummary_집계를반환한다() {
        setFixedClock(LocalDateTime.of(2026, 3, 29, 18, 0));
        when(memberRepository.countByJoinedAtGreaterThanEqualAndJoinedAtLessThan(any(), any())).thenReturn(12L);
        when(memberRepository.count()).thenReturn(4831L);
        when(memberRepository.countByIsAdminTrue()).thenReturn(4L);
        when(partyRepository.countByStatus(PartyStatus.OPEN)).thenReturn(17L);
        when(inquiryRepository.countByStatus(InquiryStatus.PENDING)).thenReturn(9L);
        when(reportRepository.countByStatus(ReportStatus.PENDING)).thenReturn(3L);

        AdminDashboardSummaryResponse response = adminDashboardService.getSummary();

        assertEquals(12, response.newMembersToday());
        assertEquals(4831, response.totalMembers());
        assertEquals(LocalDateTime.of(2026, 3, 29, 18, 0), response.generatedAt());
        verify(memberRepository).countByJoinedAtGreaterThanEqualAndJoinedAtLessThan(
                LocalDateTime.of(2026, 3, 29, 0, 0),
                LocalDateTime.of(2026, 3, 29, 18, 0, 0, 1)
        );
    }

    @Test
    void getSummary_자정경계에서도_같은시점기준으로오늘시작을계산한다() {
        setAdvancingClock(
                LocalDateTime.of(2026, 3, 29, 23, 59, 59, 999_999_999),
                LocalDateTime.of(2026, 3, 30, 0, 0)
        );
        when(memberRepository.countByJoinedAtGreaterThanEqualAndJoinedAtLessThan(any(), any())).thenReturn(1L);
        when(memberRepository.count()).thenReturn(4831L);
        when(memberRepository.countByIsAdminTrue()).thenReturn(4L);
        when(partyRepository.countByStatus(PartyStatus.OPEN)).thenReturn(17L);
        when(inquiryRepository.countByStatus(InquiryStatus.PENDING)).thenReturn(9L);
        when(reportRepository.countByStatus(ReportStatus.PENDING)).thenReturn(3L);

        AdminDashboardSummaryResponse response = adminDashboardService.getSummary();

        assertEquals(LocalDateTime.of(2026, 3, 29, 23, 59, 59, 999_999_999), response.generatedAt());
        verify(memberRepository).countByJoinedAtGreaterThanEqualAndJoinedAtLessThan(
                LocalDateTime.of(2026, 3, 29, 0, 0),
                LocalDateTime.of(2026, 3, 30, 0, 0)
        );
    }

    @Test
    void getActivity_7일과30일버킷을_서울기준으로생성한다() {
        setFixedClock(LocalDateTime.of(2026, 3, 29, 18, 0));
        stubMemberCounts(Map.of(
                LocalDate.of(2026, 2, 28), 3L,
                LocalDate.of(2026, 3, 23), 4L,
                LocalDate.of(2026, 3, 29), 7L
        ));
        stubInquiryCounts(Map.of(
                LocalDate.of(2026, 3, 23), 2L,
                LocalDate.of(2026, 3, 29), 1L
        ));
        stubReportCounts(Map.of(
                LocalDate.of(2026, 3, 23), 1L,
                LocalDate.of(2026, 3, 29), 2L
        ));
        stubPartyCounts(Map.of(
                LocalDate.of(2026, 2, 28), 1L,
                LocalDate.of(2026, 3, 23), 6L,
                LocalDate.of(2026, 3, 29), 4L
        ));

        AdminDashboardActivityResponse sevenDays = adminDashboardService.getActivity(7);
        AdminDashboardActivityResponse thirtyDays = adminDashboardService.getActivity(30);

        assertEquals(7, sevenDays.series().size());
        assertEquals(LocalDate.of(2026, 3, 23), sevenDays.series().getFirst().date());
        assertEquals(LocalDate.of(2026, 3, 29), sevenDays.series().getLast().date());
        assertEquals(7, sevenDays.series().getLast().newMembers());
        assertEquals(4, sevenDays.series().getLast().partiesCreated());

        assertEquals(30, thirtyDays.series().size());
        assertEquals(LocalDate.of(2026, 2, 28), thirtyDays.series().getFirst().date());
        assertEquals(LocalDate.of(2026, 3, 29), thirtyDays.series().getLast().date());
        assertEquals(3, thirtyDays.series().getFirst().newMembers());
    }

    @Test
    void getRecentItems_createdAtDesc로정렬한다() {
        stubRecentItems(10);

        List<AdminDashboardRecentItemResponse> response = adminDashboardService.getRecentItems(10);

        assertEquals(5, response.size());
        assertEquals("inquiry-1", response.get(0).id());
        assertEquals("report-1", response.get(1).id());
        assertEquals("notice-1", response.get(2).id());
        assertEquals("party-1", response.get(3).id());
        assertEquals("inquiry-2", response.get(4).id());
    }

    @Test
    void getRecentItems_limit을적용하고_범위를검증한다() {
        stubRecentItems(2);

        List<AdminDashboardRecentItemResponse> limited = adminDashboardService.getRecentItems(2);

        assertEquals(2, limited.size());
        assertEquals("inquiry-1", limited.getFirst().id());
        assertEquals("report-1", limited.get(1).id());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminDashboardService.getRecentItems(31)
        );
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("limit는 1 이상 30 이하여야 합니다.", exception.getMessage());
    }

    private void setFixedClock(LocalDateTime value) {
        ReflectionTestUtils.setField(
                adminDashboardService,
                "clock",
                Clock.fixed(value.atZone(SEOUL_ZONE).toInstant(), SEOUL_ZONE)
        );
    }

    private void setAdvancingClock(LocalDateTime... values) {
        ReflectionTestUtils.setField(
                adminDashboardService,
                "clock",
                new Clock() {
                    private int index = 0;

                    @Override
                    public ZoneId getZone() {
                        return SEOUL_ZONE;
                    }

                    @Override
                    public Clock withZone(ZoneId zone) {
                        return this;
                    }

                    @Override
                    public Instant instant() {
                        LocalDateTime current = values[Math.min(index, values.length - 1)];
                        if (index < values.length - 1) {
                            index++;
                        }
                        return current.atZone(SEOUL_ZONE).toInstant();
                    }
                }
        );
    }

    private void stubMemberCounts(Map<LocalDate, Long> countsByDate) {
        when(memberRepository.countByJoinedAtGreaterThanEqualAndJoinedAtLessThan(any(), any()))
                .thenAnswer(invocation -> countsByDate.getOrDefault(
                        ((LocalDateTime) invocation.getArgument(0)).toLocalDate(),
                        0L
                ));
    }

    private void stubInquiryCounts(Map<LocalDate, Long> countsByDate) {
        when(inquiryRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
                .thenAnswer(invocation -> countsByDate.getOrDefault(
                        ((LocalDateTime) invocation.getArgument(0)).toLocalDate(),
                        0L
                ));
    }

    private void stubReportCounts(Map<LocalDate, Long> countsByDate) {
        when(reportRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
                .thenAnswer(invocation -> countsByDate.getOrDefault(
                        ((LocalDateTime) invocation.getArgument(0)).toLocalDate(),
                        0L
                ));
    }

    private void stubPartyCounts(Map<LocalDate, Long> countsByDate) {
        when(partyRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
                .thenAnswer(invocation -> countsByDate.getOrDefault(
                        ((LocalDateTime) invocation.getArgument(0)).toLocalDate(),
                        0L
                ));
    }

    private void stubRecentItems(int limit) {
        setFixedClock(LocalDateTime.of(2026, 3, 29, 18, 0));
        PageRequest pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(inquiryRepository.findAllByOrderByCreatedAtDesc(any()))
                .thenReturn(new PageImpl<>(List.of(
                        inquiry("inquiry-1", LocalDateTime.of(2026, 3, 29, 17, 0)),
                        inquiry("inquiry-2", LocalDateTime.of(2026, 3, 29, 15, 0))
                ), pageable, 2));
        when(reportRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(
                        report("report-1", LocalDateTime.of(2026, 3, 29, 16, 50))
                ), pageable, 1));
        when(appNoticeRepository.findRecentPublishedForAdmin(any(), any()))
                .thenReturn(List.of(appNotice("notice-1", LocalDateTime.of(2026, 3, 29, 16, 30))));
        when(partyRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(
                        party("party-1", LocalDateTime.of(2026, 3, 29, 16, 10))
                ), pageable, 1));
    }

    private Inquiry inquiry(String id, LocalDateTime createdAt) {
        Inquiry inquiry = Inquiry.create(
                InquiryType.ACCOUNT,
                "계정 문의",
                "문의 내용",
                List.of(),
                "member-1",
                "user@sungkyul.ac.kr",
                "스쿠리유저",
                "홍길동",
                "2023112233"
        );
        ReflectionTestUtils.setField(inquiry, "id", id);
        ReflectionTestUtils.setField(inquiry, "createdAt", createdAt);
        return inquiry;
    }

    private Report report(String id, LocalDateTime createdAt) {
        Report report = Report.create(
                ReportTargetType.POST,
                "post-1",
                "member-2",
                "SPAM",
                "광고성 게시글입니다.",
                "member-1"
        );
        ReflectionTestUtils.setField(report, "id", id);
        ReflectionTestUtils.setField(report, "createdAt", createdAt);
        return report;
    }

    private AppNotice appNotice(String id, LocalDateTime createdAt) {
        AppNotice notice = AppNotice.create(
                "긴급 점검 안내",
                "점검 공지",
                AppNoticeCategory.GENERAL,
                AppNoticePriority.HIGH,
                List.of(),
                null,
                LocalDateTime.of(2026, 3, 29, 16, 0)
        );
        ReflectionTestUtils.setField(notice, "id", id);
        ReflectionTestUtils.setField(notice, "createdAt", createdAt);
        return notice;
    }

    private Party party(String id, LocalDateTime createdAt) {
        Party party = Party.create(
                "leader-1",
                Location.of("성결대학교", null, null),
                Location.of("안양역", null, null),
                LocalDateTime.of(2026, 3, 29, 18, 30),
                4,
                List.of(),
                "정문 출발"
        );
        ReflectionTestUtils.setField(party, "id", id);
        ReflectionTestUtils.setField(party, "createdAt", createdAt);
        return party;
    }
}
