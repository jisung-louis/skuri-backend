package com.skuri.skuri_backend.domain.notice.controller;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeBookmarkSummaryResponse;
import com.skuri.skuri_backend.domain.notice.service.NoticeService;
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
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MemberNoticeController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class MemberNoticeControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoticeService noticeService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getMyNoticeBookmarks_정상요청_200() throws Exception {
        mockValidToken();
        when(noticeService.getMyBookmarks("firebase-uid", 0, 20))
                .thenReturn(PageResponse.from(new org.springframework.data.domain.PageImpl<>(List.of(bookmarkSummaryResponse()))));

        mockMvc.perform(
                        get("/v1/members/me/notice-bookmarks")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("notice-1"))
                .andExpect(jsonPath("$.data.content[0].rssPreview").value("공지 미리보기"));
    }

    @Test
    void getMyNoticeBookmarks_페이지검증실패_422() throws Exception {
        mockValidToken();
        when(noticeService.getMyBookmarks("firebase-uid", -1, 20))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "page는 0 이상이어야 합니다."));

        mockMvc.perform(
                        get("/v1/members/me/notice-bookmarks")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("page", "-1")
                                .param("size", "20")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void 보호API_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/members/me/notice-bookmarks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(noticeService);
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
    }

    private NoticeBookmarkSummaryResponse bookmarkSummaryResponse() {
        return new NoticeBookmarkSummaryResponse(
                "notice-1",
                "공지 제목",
                "공지 미리보기",
                "학사",
                LocalDateTime.now()
        );
    }
}
