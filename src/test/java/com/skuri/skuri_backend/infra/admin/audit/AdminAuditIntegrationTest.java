package com.skuri.skuri_backend.infra.admin.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.skuri.skuri_backend.domain.academic.entity.Course;
import com.skuri.skuri_backend.domain.academic.repository.AcademicScheduleRepository;
import com.skuri.skuri_backend.domain.academic.repository.CourseRepository;
import com.skuri.skuri_backend.domain.campus.entity.CampusBanner;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionTarget;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionType;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerPaletteKey;
import com.skuri.skuri_backend.domain.campus.repository.CampusBannerRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.entity.AppVersion;
import com.skuri.skuri_backend.domain.support.entity.Inquiry;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.entity.InquiryType;
import com.skuri.skuri_backend.domain.support.repository.AppVersionRepository;
import com.skuri.skuri_backend.domain.support.repository.InquiryRepository;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=update")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AppVersionRepository appVersionRepository;

    @Autowired
    private CampusBannerRepository campusBannerRepository;

    @Autowired
    private AcademicScheduleRepository academicScheduleRepository;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoSpyBean
    private AdminAuditSnapshotFactory adminAuditSnapshotFactory;

    @BeforeEach
    void setUp() {
        adminAuditLogRepository.deleteAll();
        appVersionRepository.deleteAll();
        courseRepository.deleteAll();
        inquiryRepository.deleteAll();
        academicScheduleRepository.deleteAll();
        campusBannerRepository.deleteAll();
        memberRepository.deleteAll();
        reset(adminAuditSnapshotFactory);
    }

    @Test
    void 문의상태변경_감사로그를_남긴다() throws Exception {
        Member admin = saveAdminMember("admin-uid");
        Inquiry inquiry = inquiryRepository.save(Inquiry.create(
                InquiryType.BUG,
                "채팅 오류",
                "채팅 진입 시 앱이 종료됩니다.",
                java.util.List.of(),
                "member-1",
                "member-1@sungkyul.ac.kr",
                "스쿠리유저",
                "홍길동",
                "20201234"
        ));

        mockAdminToken(admin.getId());

        mockMvc.perform(
                        patch("/v1/admin/inquiries/{inquiryId}/status", inquiry.getId())
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "RESOLVED",
                                          "memo": "재현 후 수정 배포 완료"
                                        }
                                        """)
                )
                .andExpect(status().isOk());

        AdminAuditLog auditLog = latestAuditLog();
        assertThat(auditLog.getActorId()).isEqualTo("admin-uid");
        assertThat(auditLog.getAction()).isEqualTo(AdminAuditActions.INQUIRY_STATUS_UPDATED);
        assertThat(auditLog.getTargetType()).isEqualTo(AdminAuditTargetTypes.INQUIRY);
        assertThat(auditLog.getTargetId()).isEqualTo(inquiry.getId());

        JsonNode before = auditLog.getDiffBefore();
        JsonNode after = auditLog.getDiffAfter();
        assertThat(before.get("status").asText()).isEqualTo(InquiryStatus.PENDING.name());
        assertThat(before.path("memo").isNull()).isTrue();
        assertThat(after.get("status").asText()).isEqualTo(InquiryStatus.RESOLVED.name());
        assertThat(after.get("memo").asText()).isEqualTo("재현 후 수정 배포 완료");
        assertThat(after.get("subject").asText()).isEqualTo("채팅 오류");
    }

    @Test
    void 학사일정생성_감사로그를_남긴다() throws Exception {
        Member admin = saveAdminMember("admin-uid");
        mockAdminToken(admin.getId());

        mockMvc.perform(
                        post("/v1/admin/academic-schedules")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "중간고사",
                                          "startDate": "2026-04-15",
                                          "endDate": "2026-04-21",
                                          "type": "MULTI",
                                          "isPrimary": true,
                                          "description": "2026학년도 1학기 중간고사"
                                        }
                                        """)
                )
                .andExpect(status().isCreated());

        AdminAuditLog auditLog = latestAuditLog();
        assertThat(auditLog.getActorId()).isEqualTo("admin-uid");
        assertThat(auditLog.getAction()).isEqualTo(AdminAuditActions.ACADEMIC_SCHEDULE_CREATED);
        assertThat(auditLog.getTargetType()).isEqualTo(AdminAuditTargetTypes.ACADEMIC_SCHEDULE);
        assertThat(auditLog.getTargetId()).isNotBlank();
        assertThat(auditLog.getDiffBefore()).isNull();
        assertThat(auditLog.getDiffAfter().get("title").asText()).isEqualTo("중간고사");
        assertThat(auditLog.getDiffAfter().get("startDate").asText()).isEqualTo(LocalDate.of(2026, 4, 15).toString());
    }

    @Test
    void 캠퍼스배너생성_감사로그를_남긴다() throws Exception {
        Member admin = saveAdminMember("admin-uid");
        mockAdminToken(admin.getId());

        mockMvc.perform(
                        post("/v1/admin/campus-banners")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "badgeLabel": "택시 파티",
                                          "titleLabel": "택시 동승 매칭",
                                          "descriptionLabel": "같은 방향 가는 학생과 택시비를 함께 나눠요",
                                          "buttonLabel": "파티 찾기",
                                          "paletteKey": "GREEN",
                                          "imageUrl": "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg",
                                          "actionType": "IN_APP",
                                          "actionTarget": "TAXI_MAIN",
                                          "actionParams": null,
                                          "actionUrl": null,
                                          "isActive": true,
                                          "displayStartAt": "2026-03-25T00:00:00",
                                          "displayEndAt": null
                                        }
                                        """)
                )
                .andExpect(status().isCreated());

        AdminAuditLog auditLog = latestAuditLog();
        assertThat(auditLog.getActorId()).isEqualTo("admin-uid");
        assertThat(auditLog.getAction()).isEqualTo(AdminAuditActions.CAMPUS_BANNER_CREATED);
        assertThat(auditLog.getTargetType()).isEqualTo(AdminAuditTargetTypes.CAMPUS_BANNER);
        assertThat(auditLog.getTargetId()).isNotBlank();
        assertThat(auditLog.getDiffBefore()).isNull();
        assertThat(auditLog.getDiffAfter().get("titleLabel").asText()).isEqualTo("택시 동승 매칭");
        assertThat(auditLog.getDiffAfter().get("displayOrder").asInt()).isEqualTo(1);
    }

    @Test
    void 강의Bulk감사로그는_trim된학기키를_기록한다() throws Exception {
        Member admin = saveAdminMember("admin-uid");
        Course existingCourse = Course.create(
                2,
                "전공선택",
                "01255",
                "001",
                "민법총칙",
                3,
                "문상혁",
                "영401",
                null,
                "2026-1",
                "법학과"
        );
        existingCourse.appendSchedule(1, 3, 4);
        courseRepository.saveAndFlush(existingCourse);
        mockAdminToken(admin.getId());

        mockMvc.perform(
                        post("/v1/admin/courses/bulk")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "semester": " 2026-1 ",
                                          "courses": [
                                            {
                                              "code": "01255",
                                              "division": "001",
                                              "name": "민법총칙",
                                              "credits": 3,
                                              "professor": "문상혁",
                                              "department": "법학과",
                                              "grade": 2,
                                              "category": "전공선택",
                                              "location": "영401",
                                              "note": null,
                                              "schedule": [
                                                { "dayOfWeek": 1, "startPeriod": 3, "endPeriod": 4 }
                                              ]
                                            }
                                          ]
                                        }
                                        """)
                )
                .andExpect(status().isOk());

        AdminAuditLog auditLog = latestAuditLog();
        assertThat(auditLog.getTargetId()).isEqualTo("2026-1");
        assertThat(auditLog.getDiffBefore().get("semester").asText()).isEqualTo("2026-1");
        assertThat(auditLog.getDiffAfter().get("semester").asText()).isEqualTo("2026-1");
    }

    @Test
    void 강의삭제감사로그는_trim된학기키를_기록한다() throws Exception {
        Member admin = saveAdminMember("admin-uid");
        Course existingCourse = Course.create(
                2,
                "전공선택",
                "01255",
                "001",
                "민법총칙",
                3,
                "문상혁",
                "영401",
                null,
                "2026-1",
                "법학과"
        );
        existingCourse.appendSchedule(1, 3, 4);
        courseRepository.saveAndFlush(existingCourse);
        mockAdminToken(admin.getId());

        mockMvc.perform(
                        delete("/v1/admin/courses")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("semester", " 2026-1 ")
                )
                .andExpect(status().isOk());

        AdminAuditLog auditLog = latestAuditLog();
        assertThat(auditLog.getTargetId()).isEqualTo("2026-1");
        assertThat(auditLog.getDiffBefore().get("semester").asText()).isEqualTo("2026-1");
        assertThat(auditLog.getDiffAfter()).isNull();
    }

    @Test
    void 앱버전감사로그는_canonical_platform을_기록한다() throws Exception {
        Member admin = saveAdminMember("admin-uid");
        appVersionRepository.saveAndFlush(AppVersion.create(
                "ios",
                "1.0.0",
                false,
                "기존 안내",
                "기존 제목",
                false,
                null,
                null
        ));
        mockAdminToken(admin.getId());

        mockMvc.perform(
                        put("/v1/admin/app-versions/{platform}", "IOS")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "minimumVersion": "1.6.0",
                                          "forceUpdate": true,
                                          "title": "필수 업데이트 안내",
                                          "message": "안정성 개선을 위한 필수 업데이트입니다.",
                                          "showButton": true,
                                          "buttonText": "업데이트",
                                          "buttonUrl": "https://apps.apple.com/..."
                                        }
                                        """)
                )
                .andExpect(status().isOk());

        AdminAuditLog auditLog = latestAuditLog();
        assertThat(auditLog.getTargetId()).isEqualTo("ios");
        assertThat(auditLog.getDiffBefore().get("platform").asText()).isEqualTo("ios");
        assertThat(auditLog.getDiffBefore().get("minimumVersion").asText()).isEqualTo("1.0.0");
        assertThat(auditLog.getDiffAfter().get("platform").asText()).isEqualTo("ios");
        assertThat(auditLog.getDiffAfter().get("minimumVersion").asText()).isEqualTo("1.6.0");
    }

    @Test
    void 캠퍼스배너순서변경_감사로그를_남긴다() throws Exception {
        Member admin = saveAdminMember("admin-uid");
        CampusBanner first = campusBannerRepository.saveAndFlush(campusBanner("택시 동승 매칭", 1));
        CampusBanner second = campusBannerRepository.saveAndFlush(campusBanner("학교 공지사항", 2));
        CampusBanner third = campusBannerRepository.saveAndFlush(campusBanner("나의 시간표", 3));
        mockAdminToken(admin.getId());

        mockMvc.perform(
                        put("/v1/admin/campus-banners/order")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "bannerIds": [
                                            "%s",
                                            "%s",
                                            "%s"
                                          ]
                                        }
                                        """.formatted(second.getId(), first.getId(), third.getId()))
                )
                .andExpect(status().isOk());

        AdminAuditLog auditLog = latestAuditLog();
        assertThat(auditLog.getActorId()).isEqualTo("admin-uid");
        assertThat(auditLog.getAction()).isEqualTo(AdminAuditActions.CAMPUS_BANNER_REORDERED);
        assertThat(auditLog.getTargetType()).isEqualTo(AdminAuditTargetTypes.CAMPUS_BANNER);
        assertThat(auditLog.getTargetId()).isEqualTo("display-order");

        JsonNode before = auditLog.getDiffBefore();
        JsonNode after = auditLog.getDiffAfter();
        assertThat(before.isArray()).isTrue();
        assertThat(after.isArray()).isTrue();
        assertThat(before.get(0).get("id").asText()).isEqualTo(first.getId());
        assertThat(after.get(0).get("id").asText()).isEqualTo(second.getId());
        assertThat(after.get(0).get("displayOrder").asInt()).isEqualTo(1);
    }

    @Test
    void 비관리자403에서는_before_snapshot을_조회하지않는다() throws Exception {
        Member member = saveMember("member-uid", false);
        Inquiry inquiry = inquiryRepository.save(Inquiry.create(
                InquiryType.BUG,
                "채팅 오류",
                "채팅 진입 시 앱이 종료됩니다.",
                java.util.List.of(),
                member.getId(),
                "member-1@sungkyul.ac.kr",
                "스쿠리유저",
                "홍길동",
                "20201234"
        ));

        mockMemberToken(member.getId(), "member-token", "일반회원");

        mockMvc.perform(
                        patch("/v1/admin/inquiries/{inquiryId}/status", inquiry.getId())
                                .header(AUTHORIZATION, "Bearer member-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "RESOLVED",
                                          "memo": "권한 없음"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminAuditSnapshotFactory);
        assertThat(adminAuditLogRepository.count()).isZero();
    }

    private Member saveAdminMember(String memberId) {
        return saveMember(memberId, true);
    }

    private Member saveMember(String memberId, boolean admin) {
        Member member = Member.create(memberId, memberId + "@sungkyul.ac.kr", admin ? "관리자" : "일반회원", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(member, "isAdmin", admin);
        return memberRepository.save(member);
    }

    private void mockAdminToken(String uid) {
        mockMemberToken(uid, "admin-token", "관리자");
    }

    private void mockMemberToken(String uid, String token, String name) {
        when(firebaseTokenVerifier.verify(token))
                .thenReturn(new FirebaseTokenClaims(
                        uid,
                        uid + "@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        name,
                        "https://example.com/profile.jpg"
                ));
    }

    private AdminAuditLog latestAuditLog() {
        return adminAuditLogRepository.findAll().stream()
                .reduce((first, second) -> second)
                .orElseThrow();
    }

    private CampusBanner campusBanner(String titleLabel, int displayOrder) {
        return CampusBanner.create(
                "배지",
                titleLabel,
                "설명",
                "바로가기",
                CampusBannerPaletteKey.GREEN,
                "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner.jpg",
                CampusBannerActionType.IN_APP,
                CampusBannerActionTarget.TAXI_MAIN,
                null,
                null,
                true,
                LocalDateTime.of(2026, 3, 25, 0, 0),
                null,
                displayOrder
        );
    }
}
