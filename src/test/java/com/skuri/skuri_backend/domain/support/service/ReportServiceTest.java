package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateReportStatusRequest;
import com.skuri.skuri_backend.domain.support.dto.response.AdminReportResponse;
import com.skuri.skuri_backend.domain.support.entity.Report;
import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import com.skuri.skuri_backend.domain.support.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void updateReportStatus_reviewingToActioned_성공() {
        Report report = Report.create(
                ReportTargetType.POST,
                "post-1",
                "author-1",
                "SPAM",
                "광고성 게시글입니다.",
                "user-1"
        );
        ReflectionTestUtils.setField(report, "id", "report-1");
        report.updateReview(ReportStatus.REVIEWING, null, null);
        when(reportRepository.findById("report-1")).thenReturn(Optional.of(report));

        AdminReportResponse response = reportService.updateReportStatus(
                "report-1",
                new UpdateReportStatusRequest(ReportStatus.ACTIONED, "DELETE_POST", "게시글 삭제 완료")
        );

        assertEquals(ReportStatus.ACTIONED, response.status());
        assertEquals("DELETE_POST", response.action());
        assertEquals("게시글 삭제 완료", response.memo());
    }

    @Test
    void updateReportStatus_actionedToReviewing_실패() {
        Report report = Report.create(
                ReportTargetType.POST,
                "post-1",
                "author-1",
                "SPAM",
                "광고성 게시글입니다.",
                "user-1"
        );
        ReflectionTestUtils.setField(report, "id", "report-1");
        report.updateReview(ReportStatus.ACTIONED, "DELETE_POST", "처리 완료");
        when(reportRepository.findById("report-1")).thenReturn(Optional.of(report));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> reportService.updateReportStatus(
                        "report-1",
                        new UpdateReportStatusRequest(ReportStatus.REVIEWING, null, null)
                )
        );

        assertEquals(ErrorCode.INVALID_REPORT_STATUS_TRANSITION, exception.getErrorCode());
    }
}
