package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.board.exception.CommentNotFoundException;
import com.skuri.skuri_backend.domain.board.exception.PostNotFoundException;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.dto.request.CreateReportRequest;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateReportStatusRequest;
import com.skuri.skuri_backend.domain.support.dto.response.AdminReportResponse;
import com.skuri.skuri_backend.domain.support.dto.response.ReportCreateResponse;
import com.skuri.skuri_backend.domain.support.entity.Report;
import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import com.skuri.skuri_backend.domain.support.exception.ReportNotFoundException;
import com.skuri.skuri_backend.domain.support.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ReportCreateResponse createReport(String reporterId, CreateReportRequest request) {
        String normalizedTargetId = normalizeRequired(request.targetId());
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, request.targetType(), normalizedTargetId)) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_SUBMITTED);
        }

        String targetAuthorId = resolveTargetAuthorId(request.targetType(), normalizedTargetId);
        if (reporterId.equals(targetAuthorId)) {
            throw new BusinessException(ErrorCode.CANNOT_REPORT_YOURSELF);
        }

        try {
            Report report = reportRepository.saveAndFlush(Report.create(
                    request.targetType(),
                    normalizedTargetId,
                    targetAuthorId,
                    normalizeCode(request.category()),
                    normalizeRequired(request.reason()),
                    reporterId
            ));
            return new ReportCreateResponse(report.getId(), report.getStatus(), report.getCreatedAt());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_SUBMITTED);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminReportResponse> getAdminReports(ReportStatus status, ReportTargetType targetType, int page, int size) {
        Page<AdminReportResponse> reportPage = reportRepository.search(status, targetType, resolvePageable(page, size))
                .map(this::toAdminResponse);
        return PageResponse.from(reportPage);
    }

    @Transactional
    public AdminReportResponse updateReportStatus(String reportId, UpdateReportStatusRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(ReportNotFoundException::new);
        report.updateReview(request.status(), normalizeOptionalCode(request.action()), trimToNull(request.memo()));
        reportRepository.saveAndFlush(report);
        return toAdminResponse(report);
    }

    private String resolveTargetAuthorId(ReportTargetType targetType, String targetId) {
        return switch (targetType) {
            case POST -> postRepository.findByIdAndDeletedFalse(targetId)
                    .orElseThrow(PostNotFoundException::new)
                    .getAuthorId();
            case COMMENT -> commentRepository.findActiveById(targetId)
                    .orElseThrow(CommentNotFoundException::new)
                    .getAuthorId();
            case MEMBER -> memberRepository.findById(targetId)
                    .orElseThrow(MemberNotFoundException::new)
                    .getId();
        };
    }

    private AdminReportResponse toAdminResponse(Report report) {
        return new AdminReportResponse(
                report.getId(),
                report.getReporterId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getTargetAuthorId(),
                report.getCategory(),
                report.getReason(),
                report.getStatus(),
                report.getAction(),
                report.getAdminMemo(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    private Pageable resolvePageable(int page, int size) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1 이상 100 이하여야 합니다.");
        }
        return PageRequest.of(page, size);
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeCode(String value) {
        return normalizeRequired(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalCode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
