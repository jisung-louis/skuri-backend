package com.skuri.skuri_backend.domain.board.controller;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.board.constant.BoardModerationStatus;
import com.skuri.skuri_backend.domain.board.dto.request.UpdateBoardModerationRequest;
import com.skuri.skuri_backend.domain.board.dto.response.AdminCommentSummaryResponse;
import com.skuri.skuri_backend.domain.board.dto.response.AdminPostDetailResponse;
import com.skuri.skuri_backend.domain.board.dto.response.AdminPostSummaryResponse;
import com.skuri.skuri_backend.domain.board.dto.response.BoardModerationResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostImageResponse;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.service.BoardAdminService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BoardAdminController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class BoardAdminControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardAdminService boardAdminService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private com.skuri.skuri_backend.domain.member.repository.MemberRepository memberRepository;

    @Test
    void getAdminPosts_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(boardAdminService.getAdminPosts("택시", "GENERAL", "VISIBLE", "member-1", 0, 20))
                .thenReturn(PageResponse.<AdminPostSummaryResponse>builder()
                        .content(List.of(adminPostSummaryResponse()))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .totalPages(1)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build());

        mockMvc.perform(
                        get("/v1/admin/posts")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("query", "택시")
                                .param("category", "GENERAL")
                                .param("moderationStatus", "VISIBLE")
                                .param("authorId", "member-1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("post-1"))
                .andExpect(jsonPath("$.data.content[0].authorRealname").value("홍길동"))
                .andExpect(jsonPath("$.data.content[0].moderationStatus").value("VISIBLE"));
    }

    @Test
    void getAdminPost_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(boardAdminService.getAdminPost("post-1")).thenReturn(adminPostDetailResponse());

        mockMvc.perform(
                        get("/v1/admin/posts/post-1")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("post-1"))
                .andExpect(jsonPath("$.data.images[0].url").value("https://example.com/post-1.jpg"))
                .andExpect(jsonPath("$.data.moderationStatus").value("HIDDEN"));
    }

    @Test
    void updatePostModeration_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(boardAdminService.updatePostModeration(eq("post-1"), any(UpdateBoardModerationRequest.class)))
                .thenReturn(new BoardModerationResponse("post-1", BoardModerationStatus.HIDDEN));

        mockMvc.perform(
                        patch("/v1/admin/posts/post-1/moderation")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "HIDDEN"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("post-1"))
                .andExpect(jsonPath("$.data.moderationStatus").value("HIDDEN"));
    }

    @Test
    void getAdminComments_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(boardAdminService.getAdminComments("post-1", "욕설", "HIDDEN", "member-2", 0, 20))
                .thenReturn(PageResponse.<AdminCommentSummaryResponse>builder()
                        .content(List.of(adminCommentSummaryResponse()))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .totalPages(1)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build());

        mockMvc.perform(
                        get("/v1/admin/comments")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("postId", "post-1")
                                .param("query", "욕설")
                                .param("moderationStatus", "HIDDEN")
                                .param("authorId", "member-2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("comment-1"))
                .andExpect(jsonPath("$.data.content[0].postTitle").value("관리 대상 게시글"))
                .andExpect(jsonPath("$.data.content[0].moderationStatus").value("HIDDEN"));
    }

    @Test
    void updateCommentModeration_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(boardAdminService.updateCommentModeration(eq("comment-1"), any(UpdateBoardModerationRequest.class)))
                .thenReturn(new BoardModerationResponse("comment-1", BoardModerationStatus.DELETED));

        mockMvc.perform(
                        patch("/v1/admin/comments/comment-1/moderation")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "DELETED"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("comment-1"))
                .andExpect(jsonPath("$.data.moderationStatus").value("DELETED"));
    }

    @Test
    void adminBoardApi_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(
                        get("/v1/admin/posts")
                                .header(AUTHORIZATION, "Bearer user-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void updatePostModeration_잘못된status_422() throws Exception {
        mockToken("admin-token", true);
        when(boardAdminService.updatePostModeration(eq("post-1"), any(UpdateBoardModerationRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "status는 VISIBLE, HIDDEN, DELETED 중 하나여야 합니다."));

        mockMvc.perform(
                        patch("/v1/admin/posts/post-1/moderation")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "ARCHIVED"
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("status는 VISIBLE, HIDDEN, DELETED 중 하나여야 합니다."));
    }

    @Test
    void 보호API_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/admin/posts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(boardAdminService);
    }

    private void mockToken(String token, boolean admin) {
        when(firebaseTokenVerifier.verify(token))
                .thenReturn(new FirebaseTokenClaims(
                        admin ? "admin-uid" : "user-uid",
                        (admin ? "admin" : "user") + "@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        admin ? "관리자" : "일반회원",
                        "https://example.com/profile.jpg"
                ));
        com.skuri.skuri_backend.domain.member.entity.Member member =
                com.skuri.skuri_backend.domain.member.entity.Member.create(
                        admin ? "admin-uid" : "user-uid",
                        (admin ? "admin" : "user") + "@sungkyul.ac.kr",
                        admin ? "관리자" : "일반회원",
                        LocalDateTime.now().minusDays(1)
                );
        member.updateAdminRole(admin);
        when(memberRepository.findById(admin ? "admin-uid" : "user-uid")).thenReturn(Optional.of(member));
    }

    private AdminPostSummaryResponse adminPostSummaryResponse() {
        return new AdminPostSummaryResponse(
                "post-1",
                PostCategory.GENERAL,
                "관리 대상 게시글",
                "member-1",
                "스쿠리유저",
                "홍길동",
                false,
                5,
                10,
                LocalDateTime.of(2026, 3, 29, 12, 0),
                LocalDateTime.of(2026, 3, 29, 12, 30),
                BoardModerationStatus.VISIBLE,
                "https://example.com/post-1-thumb.jpg"
        );
    }

    private AdminPostDetailResponse adminPostDetailResponse() {
        return new AdminPostDetailResponse(
                "post-1",
                PostCategory.GENERAL,
                "관리 대상 게시글",
                "관리자 상세 본문",
                "member-1",
                "스쿠리유저",
                "홍길동",
                false,
                42,
                10,
                5,
                3,
                LocalDateTime.of(2026, 3, 29, 12, 0),
                LocalDateTime.of(2026, 3, 29, 12, 30),
                BoardModerationStatus.HIDDEN,
                "https://example.com/post-1-thumb.jpg",
                List.of(new PostImageResponse("https://example.com/post-1.jpg", "https://example.com/post-1-thumb.jpg", 800, 600, 12345, "image/jpeg"))
        );
    }

    private AdminCommentSummaryResponse adminCommentSummaryResponse() {
        return new AdminCommentSummaryResponse(
                "comment-1",
                "post-1",
                "관리 대상 게시글",
                "member-2",
                "댓글유저",
                "김철수",
                "문제되는 댓글 내용 일부...",
                null,
                LocalDateTime.of(2026, 3, 29, 13, 0),
                BoardModerationStatus.HIDDEN
        );
    }
}
