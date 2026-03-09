package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.dto.request.CreateInquiryRequest;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateInquiryStatusRequest;
import com.skuri.skuri_backend.domain.support.dto.response.AdminInquiryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.InquiryCreateResponse;
import com.skuri.skuri_backend.domain.support.dto.response.InquiryResponse;
import com.skuri.skuri_backend.domain.support.entity.Inquiry;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.exception.InquiryNotFoundException;
import com.skuri.skuri_backend.domain.support.repository.InquiryRepository;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final InquiryRepository inquiryRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public InquiryCreateResponse createInquiry(AuthenticatedMember authenticatedMember, CreateInquiryRequest request) {
        Member member = memberRepository.findById(authenticatedMember.uid()).orElse(null);
        Inquiry inquiry = inquiryRepository.save(Inquiry.create(
                request.type(),
                normalizeRequired(request.subject()),
                normalizeRequired(request.content()),
                authenticatedMember.uid(),
                trimToNull(authenticatedMember.email()),
                resolveUserName(member, authenticatedMember),
                trimToNull(member != null ? member.getRealname() : authenticatedMember.providerDisplayName()),
                trimToNull(member != null ? member.getStudentId() : null)
        ));
        return new InquiryCreateResponse(inquiry.getId(), inquiry.getStatus(), inquiry.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> getMyInquiries(String memberId) {
        return inquiryRepository.findByUserIdOrderByCreatedAtDesc(memberId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminInquiryResponse> getAdminInquiries(InquiryStatus status, int page, int size) {
        Pageable pageable = resolvePageable(page, size);
        Page<AdminInquiryResponse> inquiryPage = (status == null
                ? inquiryRepository.findAllByOrderByCreatedAtDesc(pageable)
                : inquiryRepository.findByStatusOrderByCreatedAtDesc(status, pageable))
                .map(this::toAdminResponse);
        return PageResponse.from(inquiryPage);
    }

    @Transactional
    public AdminInquiryResponse updateInquiryStatus(String inquiryId, UpdateInquiryStatusRequest request) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(InquiryNotFoundException::new);
        inquiry.updateStatus(request.status(), trimToNull(request.memo()));
        inquiryRepository.saveAndFlush(inquiry);
        return toAdminResponse(inquiry);
    }

    @Transactional
    public void anonymizeWithdrawnMemberInquiries(String memberId) {
        inquiryRepository.findAllByUserId(memberId)
                .forEach(Inquiry::anonymizeUserProfile);
    }

    private InquiryResponse toResponse(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getType(),
                inquiry.getSubject(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt()
        );
    }

    private AdminInquiryResponse toAdminResponse(Inquiry inquiry) {
        return new AdminInquiryResponse(
                inquiry.getId(),
                inquiry.getUserId(),
                inquiry.getType(),
                inquiry.getSubject(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getAdminMemo(),
                inquiry.getUserEmail(),
                inquiry.getUserName(),
                inquiry.getUserRealname(),
                inquiry.getUserStudentId(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt()
        );
    }

    private String resolveUserName(Member member, AuthenticatedMember authenticatedMember) {
        if (member != null) {
            if (StringUtils.hasText(member.getNickname())) {
                return member.getNickname().trim();
            }
            if (StringUtils.hasText(member.getRealname())) {
                return member.getRealname().trim();
            }
        }
        return trimToNull(authenticatedMember.providerDisplayName());
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
