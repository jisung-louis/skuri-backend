package com.skuri.skuri_backend.domain.member.repository;

import com.skuri.skuri_backend.domain.member.constant.AdminMemberSortField;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.notification.entity.FcmToken;
import com.skuri.skuri_backend.domain.notification.repository.FcmTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class MemberRepositoryDataJpaTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Test
    void searchAdminMembers_realname오름차순을지원한다() {
        saveMember("member-b", "박하나", "b@sungkyul.ac.kr", "경영학과", "20230002",
                LocalDateTime.of(2025, 3, 2, 9, 0), LocalDateTime.of(2026, 3, 2, 10, 0), false);
        saveMember("member-a", "김하나", "a@sungkyul.ac.kr", "컴퓨터공학과", "20230001",
                LocalDateTime.of(2025, 3, 1, 9, 0), LocalDateTime.of(2026, 3, 1, 10, 0), false);

        Page<AdminMemberSummaryProjection> result = memberRepository.searchAdminMembers(
                null,
                null,
                null,
                null,
                AdminMemberSortField.REALNAME,
                Sort.Direction.ASC,
                PageRequest.of(0, 10)
        );

        assertEquals(List.of("member-a", "member-b"), result.getContent().stream().map(AdminMemberSummaryProjection::id).toList());
        assertEquals(List.of("김하나", "박하나"), result.getContent().stream().map(AdminMemberSummaryProjection::realname).toList());
    }

    @Test
    void searchAdminMembers_lastLoginOs내림차순과최신토큰매핑을지원한다() {
        saveMember("member-android", "안드로이드", "android@sungkyul.ac.kr", "컴퓨터공학과", "20230011",
                LocalDateTime.of(2025, 3, 11, 9, 0), LocalDateTime.of(2026, 3, 11, 10, 0), false);
        saveMember("member-ios", "아이오에스", "ios@sungkyul.ac.kr", "컴퓨터공학과", "20230012",
                LocalDateTime.of(2025, 3, 12, 9, 0), LocalDateTime.of(2026, 3, 12, 10, 0), false);
        saveMember("member-none", "토큰없음", "none@sungkyul.ac.kr", "컴퓨터공학과", "20230013",
                LocalDateTime.of(2025, 3, 13, 9, 0), LocalDateTime.of(2026, 3, 13, 10, 0), false);

        saveFcmToken("member-android", "token-old", "ios", LocalDateTime.of(2026, 3, 20, 10, 0));
        saveFcmToken("member-android", "token-new", "android", LocalDateTime.of(2026, 3, 21, 10, 0));
        saveFcmToken("member-ios", "token-ios", "ios", LocalDateTime.of(2026, 3, 22, 10, 0));

        Page<AdminMemberSummaryProjection> result = memberRepository.searchAdminMembers(
                null,
                null,
                null,
                null,
                AdminMemberSortField.LAST_LOGIN_OS,
                Sort.Direction.DESC,
                PageRequest.of(0, 10)
        );

        assertEquals(
                List.of("member-ios", "member-android", "member-none"),
                result.getContent().stream().map(AdminMemberSummaryProjection::id).toList()
        );
        assertEquals(
                Arrays.asList("ios", "android", null),
                result.getContent().stream().map(AdminMemberSummaryProjection::lastLoginOs).toList()
        );
    }

    @Test
    void searchAdminMembers_null값은항상마지막으로정렬한다() {
        saveMember("member-null", null, "null@sungkyul.ac.kr", "컴퓨터공학과", "20230021",
                LocalDateTime.of(2025, 3, 21, 9, 0), LocalDateTime.of(2026, 3, 21, 10, 0), false);
        saveMember("member-b", "박하나", "b@sungkyul.ac.kr", "컴퓨터공학과", "20230022",
                LocalDateTime.of(2025, 3, 22, 9, 0), LocalDateTime.of(2026, 3, 22, 10, 0), false);
        saveMember("member-a", "김하나", "a@sungkyul.ac.kr", "컴퓨터공학과", "20230023",
                LocalDateTime.of(2025, 3, 23, 9, 0), LocalDateTime.of(2026, 3, 23, 10, 0), false);

        Page<AdminMemberSummaryProjection> result = memberRepository.searchAdminMembers(
                null,
                null,
                null,
                null,
                AdminMemberSortField.REALNAME,
                Sort.Direction.ASC,
                PageRequest.of(0, 10)
        );

        assertEquals(
                List.of("member-a", "member-b", "member-null"),
                result.getContent().stream().map(AdminMemberSummaryProjection::id).toList()
        );
    }

    @Test
    void searchAdminMembers_기존필터를유지한다() {
        saveMember("member-match", "홍길동", "hong@sungkyul.ac.kr", "컴퓨터공학과", "2023112233",
                LocalDateTime.of(2025, 3, 1, 9, 0), LocalDateTime.of(2026, 3, 29, 10, 0), false);
        saveMember("member-admin", "홍길동", "admin@sungkyul.ac.kr", "컴퓨터공학과", "2023112234",
                LocalDateTime.of(2025, 3, 2, 9, 0), LocalDateTime.of(2026, 3, 29, 11, 0), true);
        saveMember("member-department", "홍길동", "dept@sungkyul.ac.kr", "경영학과", "2023112235",
                LocalDateTime.of(2025, 3, 3, 9, 0), LocalDateTime.of(2026, 3, 29, 12, 0), false);
        Member withdrawn = saveMember("member-withdrawn", "홍길동", "withdrawn@sungkyul.ac.kr", "컴퓨터공학과", "2023112236",
                LocalDateTime.of(2025, 3, 4, 9, 0), LocalDateTime.of(2026, 3, 29, 13, 0), false);
        withdrawn.withdraw(LocalDateTime.of(2026, 3, 29, 14, 0));
        memberRepository.saveAndFlush(withdrawn);

        Page<AdminMemberSummaryProjection> result = memberRepository.searchAdminMembers(
                "홍길동",
                MemberStatus.ACTIVE,
                false,
                "컴퓨터공학과",
                AdminMemberSortField.JOINED_AT,
                Sort.Direction.DESC,
                PageRequest.of(0, 10)
        );

        assertEquals(List.of("member-match"), result.getContent().stream().map(AdminMemberSummaryProjection::id).toList());
        assertEquals(1, result.getTotalElements());
    }

    private Member saveMember(
            String id,
            String realname,
            String email,
            String department,
            String studentId,
            LocalDateTime joinedAt,
            LocalDateTime lastLogin,
            boolean isAdmin
    ) {
        Member member = Member.create(id, email, realname, joinedAt);
        member.updateProfile("닉네임-" + id, studentId, department, null);
        ReflectionTestUtils.setField(member, "joinedAt", joinedAt);
        ReflectionTestUtils.setField(member, "lastLogin", lastLogin);
        ReflectionTestUtils.setField(member, "isAdmin", isAdmin);
        return memberRepository.saveAndFlush(member);
    }

    private void saveFcmToken(String userId, String token, String platform, LocalDateTime lastUsedAt) {
        FcmToken fcmToken = FcmToken.create(userId, token, platform);
        ReflectionTestUtils.setField(fcmToken, "createdAt", lastUsedAt.minusMinutes(5));
        ReflectionTestUtils.setField(fcmToken, "lastUsedAt", lastUsedAt);
        fcmTokenRepository.saveAndFlush(fcmToken);
    }
}
