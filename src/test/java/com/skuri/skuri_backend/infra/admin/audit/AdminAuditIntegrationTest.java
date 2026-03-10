package com.skuri.skuri_backend.infra.admin.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.skuri.skuri_backend.domain.academic.repository.AcademicScheduleRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.entity.Inquiry;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.entity.InquiryType;
import com.skuri.skuri_backend.domain.support.repository.InquiryRepository;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private AcademicScheduleRepository academicScheduleRepository;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @BeforeEach
    void setUp() {
        adminAuditLogRepository.deleteAll();
        inquiryRepository.deleteAll();
        academicScheduleRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void 문의상태변경_감사로그를_남긴다() throws Exception {
        Member admin = saveAdminMember("admin-uid");
        Inquiry inquiry = inquiryRepository.save(Inquiry.create(
                InquiryType.BUG,
                "채팅 오류",
                "채팅 진입 시 앱이 종료됩니다.",
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

    private Member saveAdminMember(String memberId) {
        Member member = Member.create(memberId, memberId + "@sungkyul.ac.kr", "관리자", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(member, "isAdmin", true);
        return memberRepository.save(member);
    }

    private void mockAdminToken(String uid) {
        when(firebaseTokenVerifier.verify("admin-token"))
                .thenReturn(new FirebaseTokenClaims(
                        uid,
                        uid + "@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "관리자",
                        "https://example.com/profile.jpg"
                ));
    }

    private AdminAuditLog latestAuditLog() {
        return adminAuditLogRepository.findAll().stream()
                .reduce((first, second) -> second)
                .orElseThrow();
    }
}
