package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.constant.DepartmentCatalog;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberAdminRoleRequest;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberDetailResponse;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberSummaryResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberBankAccountResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberNotificationSettingResponse;
import com.skuri.skuri_backend.domain.member.entity.BankAccount;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.member.entity.NotificationSetting;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.infra.admin.list.AdminPageRequestPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MemberAdminService {

    private static final Sort ADMIN_MEMBER_DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "joinedAt");

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminMemberSummaryResponse> getAdminMembers(
            String query,
            MemberStatus status,
            Boolean isAdmin,
            String department,
            int page,
            int size
    ) {
        Pageable pageable = resolvePageable(page, size);
        Page<AdminMemberSummaryResponse> memberPage = memberRepository.searchAdminMembers(
                        normalizeQuery(query),
                        status,
                        isAdmin,
                        normalizeDepartment(department),
                        pageable
                )
                .map(this::toSummaryResponse);
        return PageResponse.from(memberPage);
    }

    @Transactional(readOnly = true)
    public AdminMemberDetailResponse getAdminMember(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        return toDetailResponse(member);
    }

    @Transactional
    public AdminMemberDetailResponse updateAdminRole(String memberId, UpdateMemberAdminRoleRequest request) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(MemberNotFoundException::new);

        if (member.isWithdrawn()) {
            throw new BusinessException(ErrorCode.CONFLICT, "탈퇴한 회원의 관리자 권한은 변경할 수 없습니다.");
        }

        member.updateAdminRole(Boolean.TRUE.equals(request.isAdmin()));
        memberRepository.saveAndFlush(member);
        return toDetailResponse(member);
    }

    private Pageable resolvePageable(int page, int size) {
        Pageable validatedPageable = AdminPageRequestPolicy.of(page, size);
        return PageRequest.of(validatedPageable.getPageNumber(), validatedPageable.getPageSize(), ADMIN_MEMBER_DEFAULT_SORT);
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.trim();
    }

    private String normalizeDepartment(String department) {
        if (!StringUtils.hasText(department)) {
            return null;
        }
        String normalizedDepartment = DepartmentCatalog.normalize(department);
        if (normalizedDepartment == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 department입니다.");
        }
        return normalizedDepartment;
    }

    private AdminMemberSummaryResponse toSummaryResponse(Member member) {
        return new AdminMemberSummaryResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRealname(),
                member.getStudentId(),
                member.getDepartment(),
                member.isAdmin(),
                member.getJoinedAt(),
                member.getLastLogin(),
                member.getStatus()
        );
    }

    private AdminMemberDetailResponse toDetailResponse(Member member) {
        return new AdminMemberDetailResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRealname(),
                member.getStudentId(),
                member.getDepartment(),
                member.getPhotoUrl(),
                member.isAdmin(),
                member.getStatus(),
                member.getJoinedAt(),
                member.getLastLogin(),
                member.getWithdrawnAt(),
                toBankAccountResponse(member.getBankAccount()),
                toNotificationSettingResponse(member.getNotificationSetting())
        );
    }

    private MemberBankAccountResponse toBankAccountResponse(BankAccount bankAccount) {
        if (bankAccount == null) {
            return null;
        }
        return new MemberBankAccountResponse(
                bankAccount.getBankName(),
                bankAccount.getAccountNumber(),
                bankAccount.getAccountHolder(),
                bankAccount.getHideName()
        );
    }

    private MemberNotificationSettingResponse toNotificationSettingResponse(NotificationSetting notificationSetting) {
        if (notificationSetting == null) {
            notificationSetting = NotificationSetting.defaultSetting();
        }
        Map<String, Boolean> detail = notificationSetting.getNoticeNotificationsDetail() != null
                ? new HashMap<>(notificationSetting.getNoticeNotificationsDetail())
                : Map.of();

        return new MemberNotificationSettingResponse(
                notificationSetting.isAllNotifications(),
                notificationSetting.isPartyNotifications(),
                notificationSetting.isNoticeNotifications(),
                notificationSetting.isBoardLikeNotifications(),
                notificationSetting.isCommentNotifications(),
                notificationSetting.isBookmarkedPostCommentNotifications(),
                notificationSetting.isSystemNotifications(),
                notificationSetting.isAcademicScheduleNotifications(),
                notificationSetting.isAcademicScheduleDayBeforeEnabled(),
                notificationSetting.isAcademicScheduleAllEventsEnabled(),
                detail
        );
    }
}
