package com.skuri.skuri_backend.domain.member.controller;

import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberProfileRequest;
import com.skuri.skuri_backend.domain.member.dto.response.MemberBankAccountResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberCreateResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberMeResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberNotificationSettingResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberPublicProfileResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberUpsertResult;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.service.MemberService;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.auth.config.SecurityConfig;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseAuthenticationFilter;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MemberController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class MemberControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void postMembers_신규생성_201_응답스키마검증() throws Exception {
        mockValidToken();
        when(memberService.createMember(any()))
                .thenReturn(MemberUpsertResult.created(memberCreateResponse()));

        mockMvc.perform(
                        post("/v1/members")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("firebase-uid"))
                .andExpect(jsonPath("$.data.notificationSetting").doesNotExist())
                .andExpect(jsonPath("$.data.lastLogin").doesNotExist());
    }

    @Test
    void postMembers_중복호출_200_응답스키마검증() throws Exception {
        mockValidToken();
        when(memberService.createMember(any()))
                .thenReturn(MemberUpsertResult.existing(memberCreateResponse()));

        mockMvc.perform(
                        post("/v1/members")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("firebase-uid"))
                .andExpect(jsonPath("$.data.notificationSetting").doesNotExist())
                .andExpect(jsonPath("$.data.lastLogin").doesNotExist());
    }

    @Test
    void getMembersMe_lastLogin포함() throws Exception {
        mockValidToken();
        when(memberService.getMyProfile("firebase-uid")).thenReturn(memberMeResponse());

        mockMvc.perform(
                        get("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastLogin").exists());
    }

    @Test
    void getMembersById_정상토큰_공개프로필반환() throws Exception {
        mockValidToken();
        when(memberService.getMemberById("target-uid"))
                .thenReturn(new MemberPublicProfileResponse(
                        "target-uid",
                        "공개닉네임",
                        "컴퓨터공학과",
                        "https://example.com/target.jpg"
                ));

        mockMvc.perform(
                        get("/v1/members/target-uid")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("target-uid"))
                .andExpect(jsonPath("$.data.nickname").value("공개닉네임"))
                .andExpect(jsonPath("$.data.department").value("컴퓨터공학과"))
                .andExpect(jsonPath("$.data.photoUrl").value("https://example.com/target.jpg"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.bankAccount").doesNotExist())
                .andExpect(jsonPath("$.data.notificationSetting").doesNotExist());
    }

    @Test
    void getMembersById_회원없음_404() throws Exception {
        mockValidToken();
        when(memberService.getMemberById("not-found"))
                .thenThrow(new MemberNotFoundException());

        mockMvc.perform(
                        get("/v1/members/not-found")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void patchMembersMe_프로필수정_요청필드만반영() throws Exception {
        mockValidToken();
        when(memberService.updateMyProfile(eq("firebase-uid"), any(UpdateMemberProfileRequest.class)))
                .thenReturn(memberMeResponse());

        mockMvc.perform(
                        patch("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "nickname": "새닉네임",
                                          "studentId": "20201234",
                                          "department": "컴퓨터공학과",
                                          "photoUrl": "https://example.com/profile.jpg"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realname").value("홍길동"));

        verify(memberService).updateMyProfile(
                eq("firebase-uid"),
                argThat(request ->
                        "새닉네임".equals(request.nickname())
                                && "20201234".equals(request.studentId())
                                && "컴퓨터공학과".equals(request.department())
                                && "https://example.com/profile.jpg".equals(request.photoUrl())
                )
        );
    }

    @Test
    void putMembersMeBankAccount_기본성공() throws Exception {
        mockValidToken();
        when(memberService.updateMyBankAccount(eq("firebase-uid"), any()))
                .thenReturn(memberMeResponse());

        mockMvc.perform(
                        put("/v1/members/me/bank-account")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "bankName": "카카오뱅크",
                                          "accountNumber": "3333-01-1234567",
                                          "accountHolder": "홍길동",
                                          "hideName": false
                                        }
                                        """)
                )
                .andExpect(status().isOk());
    }

    @Test
    void patchMembersMeNotificationSettings_기본성공() throws Exception {
        mockValidToken();
        when(memberService.updateMyNotificationSettings(eq("firebase-uid"), any()))
                .thenReturn(memberMeResponse());

        mockMvc.perform(
                        patch("/v1/members/me/notification-settings")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "partyNotifications": true,
                                          "commentNotifications": false,
                                          "bookmarkedPostCommentNotifications": true,
                                          "noticeNotifications": false,
                                          "noticeNotificationsDetail": {
                                            "news": true,
                                            "academy": false
                                          }
                                        }
                                        """)
                )
                .andExpect(status().isOk());
    }

    @Test
    void putMembersMeBankAccount_필수값누락_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        put("/v1/members/me/bank-account")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "bankName": " ",
                                          "accountNumber": "3333-01-1234567",
                                          "accountHolder": "홍길동",
                                          "hideName": false
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("bankName")));

        verifyNoInteractions(memberService);
    }

    @Test
    void patchMembersMe_길이초과_422() throws Exception {
        mockValidToken();
        String overSizeNickname = "a".repeat(51);

        mockMvc.perform(
                        patch("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "nickname": "%s"
                                        }
                                        """.formatted(overSizeNickname)
                                )
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("nickname")));

        verifyNoInteractions(memberService);
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "google-provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
    }

    private MemberCreateResponse memberCreateResponse() {
        return new MemberCreateResponse(
                "firebase-uid",
                "user@sungkyul.ac.kr",
                "홍길동",
                "20201234",
                "컴퓨터공학과",
                "https://example.com/profile.jpg",
                "홍길동",
                false,
                new MemberBankAccountResponse("카카오뱅크", "3333-01-1234567", "홍길동", false),
                LocalDateTime.now()
        );
    }

    private MemberMeResponse memberMeResponse() {
        return new MemberMeResponse(
                "firebase-uid",
                "user@sungkyul.ac.kr",
                "홍길동",
                "20201234",
                "컴퓨터공학과",
                "https://example.com/profile.jpg",
                "홍길동",
                false,
                new MemberBankAccountResponse("카카오뱅크", "3333-01-1234567", "홍길동", false),
                new MemberNotificationSettingResponse(
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        false,
                        Map.of("news", true, "academy", true, "scholarship", false)
                ),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
