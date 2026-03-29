package com.skuri.skuri_backend.domain.member.repository;

import com.skuri.skuri_backend.domain.member.constant.AdminMemberSortField;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface MemberRepositoryCustom {

    Page<AdminMemberSummaryProjection> searchAdminMembers(
            String query,
            MemberStatus status,
            Boolean isAdmin,
            String department,
            AdminMemberSortField sortField,
            Sort.Direction sortDirection,
            Pageable pageable
    );

    void insert(Member member);
}
