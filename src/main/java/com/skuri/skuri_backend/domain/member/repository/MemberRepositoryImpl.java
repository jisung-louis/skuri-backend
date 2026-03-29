package com.skuri.skuri_backend.domain.member.repository;

import com.skuri.skuri_backend.domain.member.constant.AdminMemberSortField;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private static final String LATEST_FCM_JOIN = """
            left join (
                select ranked.user_id, ranked.platform
                from (
                    select
                        ft.user_id,
                        ft.platform,
                        row_number() over (
                            partition by ft.user_id
                            order by coalesce(ft.last_used_at, ft.created_at) desc, ft.id desc
                        ) as rn
                    from fcm_tokens ft
                ) ranked
                where ranked.rn = 1
            ) latest_fcm on latest_fcm.user_id = m.id
            """;

    private static final String ADMIN_MEMBER_BASE_FROM_WHERE = """
            from members m
            %s
            where (:query is null
                    or lower(m.email) like lower(concat('%%', :query, '%%'))
                    or lower(coalesce(m.nickname, '')) like lower(concat('%%', :query, '%%'))
                    or lower(coalesce(m.realname, '')) like lower(concat('%%', :query, '%%'))
                    or lower(coalesce(m.student_id, '')) like lower(concat('%%', :query, '%%')))
              and (:status is null or m.status = :status)
              and (:isAdmin is null or m.is_admin = :isAdmin)
              and (:department is null or m.department = :department)
            """;

    private static final String ADMIN_MEMBER_SELECT = """
            select
                m.id,
                m.email,
                m.nickname,
                m.realname,
                m.student_id,
                m.department,
                m.is_admin,
                m.joined_at,
                m.last_login,
                latest_fcm.platform as last_login_os,
                m.status
            """;

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminMemberSummaryProjection> searchAdminMembers(
            String query,
            MemberStatus status,
            Boolean isAdmin,
            String department,
            AdminMemberSortField sortField,
            Sort.Direction sortDirection,
            Pageable pageable
    ) {
        String sortExpression = resolveSortExpression(sortField);
        String sortClause = """
                 order by
                    case when %s is null then 1 else 0 end asc,
                    %s %s,
                    m.id asc
                """.formatted(sortExpression, sortExpression, sortDirection.name());

        Query dataQuery = entityManager.createNativeQuery(
                ADMIN_MEMBER_SELECT
                        + ADMIN_MEMBER_BASE_FROM_WHERE.formatted(LATEST_FCM_JOIN)
                        + sortClause
        );
        applySearchParameters(dataQuery, query, status, isAdmin, department);
        dataQuery.setFirstResult(Math.toIntExact(pageable.getOffset()));
        dataQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();

        Query countQuery = entityManager.createNativeQuery(
                "select count(*) " + ADMIN_MEMBER_BASE_FROM_WHERE.formatted("")
        );
        applySearchParameters(countQuery, query, status, isAdmin, department);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        List<AdminMemberSummaryProjection> content = rows.stream()
                .map(this::toProjection)
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    @Transactional
    public void insert(Member member) {
        entityManager.persist(member);
        entityManager.flush();
    }

    private void applySearchParameters(
            Query query,
            String searchQuery,
            MemberStatus status,
            Boolean isAdmin,
            String department
    ) {
        query.setParameter("query", searchQuery);
        query.setParameter("status", status == null ? null : status.name());
        query.setParameter("isAdmin", isAdmin);
        query.setParameter("department", department);
    }

    private String resolveSortExpression(AdminMemberSortField sortField) {
        return switch (sortField) {
            case ID -> "m.id";
            case REALNAME -> "m.realname";
            case EMAIL -> "m.email";
            case NICKNAME -> "m.nickname";
            case DEPARTMENT -> "m.department";
            case STUDENT_ID -> "m.student_id";
            case JOINED_AT -> "m.joined_at";
            case LAST_LOGIN -> "m.last_login";
            case LAST_LOGIN_OS -> "latest_fcm.platform";
        };
    }

    private AdminMemberSummaryProjection toProjection(Object[] row) {
        return new AdminMemberSummaryProjection(
                (String) row[0],
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                (String) row[5],
                toBoolean(row[6]),
                toLocalDateTime(row[7]),
                toLocalDateTime(row[8]),
                (String) row[9],
                MemberStatus.valueOf((String) row[10])
        );
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        throw new IllegalStateException("지원하지 않는 LocalDateTime 변환 타입입니다: " + value.getClass());
    }
}
