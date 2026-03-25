package com.skuri.skuri_backend.domain.notice.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.domain.notice.dto.request.CreateNoticeCommentRequest;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeBookmarkResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeDetailResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeLikeResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeReadResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeSummaryResponse;
import com.skuri.skuri_backend.domain.notice.service.NoticeService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiNoticeExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiNoticeSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/notices")
@Tag(name = "Notice API", description = "학교 공지 조회/댓글/좋아요/북마크/읽음 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    @Operation(summary = "공지 목록 조회", description = "카테고리/검색 조건으로 공지 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeSummaryPageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNoticeExamples.SUCCESS_NOTICE_LIST_PAGE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 category",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_category", value = OpenApiNoticeExamples.ERROR_NOTICE_CATEGORY_INVALID)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "페이지네이션 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "bean_validation", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    public ResponseEntity<ApiResponse<PageResponse<NoticeSummaryResponse>>> getNotices(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getNotices(
                requireAuthenticatedMember(authenticatedMember).uid(),
                category,
                search,
                page,
                size
        )));
    }

    @GetMapping("/{noticeId}")
    @Operation(summary = "공지 상세 조회", description = "공지 상세를 조회하며 조회수를 1 증가시킵니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNoticeExamples.SUCCESS_NOTICE_DETAIL)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공지 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "notice_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> getNotice(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "공지 ID", example = "bm90aWNlLTE")
            @PathVariable String noticeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                noticeService.getNoticeDetail(requireAuthenticatedMember(authenticatedMember).uid(), noticeId)
        ));
    }

    @PostMapping("/{noticeId}/read")
    @Operation(summary = "공지 읽음 처리", description = "공지 읽음 상태를 저장합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeReadApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNoticeExamples.SUCCESS_NOTICE_READ)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공지 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "notice_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<NoticeReadResponse>> markRead(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "공지 ID", example = "bm90aWNlLTE")
            @PathVariable String noticeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                noticeService.markRead(requireAuthenticatedMember(authenticatedMember).uid(), noticeId)
        ));
    }

    @PostMapping("/{noticeId}/like")
    @Operation(summary = "공지 좋아요 등록", description = "공지 좋아요를 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeLikeApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNoticeExamples.SUCCESS_NOTICE_LIKE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공지 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "notice_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<NoticeLikeResponse>> likeNotice(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "공지 ID", example = "bm90aWNlLTE")
            @PathVariable String noticeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                noticeService.likeNotice(requireAuthenticatedMember(authenticatedMember).uid(), noticeId)
        ));
    }

    @DeleteMapping("/{noticeId}/like")
    @Operation(summary = "공지 좋아요 취소", description = "공지 좋아요를 취소합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeLikeApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNoticeExamples.SUCCESS_NOTICE_LIKE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공지 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "notice_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<NoticeLikeResponse>> unlikeNotice(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "공지 ID", example = "bm90aWNlLTE")
            @PathVariable String noticeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                noticeService.unlikeNotice(requireAuthenticatedMember(authenticatedMember).uid(), noticeId)
        ));
    }

    @PostMapping("/{noticeId}/bookmark")
    @Operation(summary = "공지 북마크 등록", description = "공지 북마크를 등록합니다. 이미 북마크한 경우에도 멱등하게 성공합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeBookmarkApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNoticeExamples.SUCCESS_NOTICE_BOOKMARK)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공지 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "notice_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<NoticeBookmarkResponse>> bookmarkNotice(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "공지 ID", example = "bm90aWNlLTE")
            @PathVariable String noticeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                noticeService.bookmarkNotice(requireAuthenticatedMember(authenticatedMember).uid(), noticeId)
        ));
    }

    @DeleteMapping("/{noticeId}/bookmark")
    @Operation(summary = "공지 북마크 취소", description = "공지 북마크를 취소합니다. 북마크가 없어도 멱등하게 성공합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeBookmarkApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNoticeExamples.SUCCESS_NOTICE_BOOKMARK_REMOVED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공지 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "notice_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<NoticeBookmarkResponse>> unbookmarkNotice(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "공지 ID", example = "bm90aWNlLTE")
            @PathVariable String noticeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                noticeService.unbookmarkNotice(requireAuthenticatedMember(authenticatedMember).uid(), noticeId)
        ));
    }

    @GetMapping("/{noticeId}/comments")
    @Operation(summary = "공지 댓글 목록 조회", description = "공지 댓글 목록을 flat list로 조회합니다. (무제한 depth)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeCommentListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNoticeExamples.SUCCESS_NOTICE_COMMENTS_LIST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공지 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "notice_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<List<NoticeCommentResponse>>> getComments(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "공지 ID", example = "bm90aWNlLTE")
            @PathVariable String noticeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                noticeService.getComments(requireAuthenticatedMember(authenticatedMember).uid(), noticeId)
        ));
    }

    @PostMapping("/{noticeId}/comments")
    @Operation(summary = "공지 댓글 작성", description = "공지 댓글/답글을 작성합니다. depth 제한은 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "작성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeCommentApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNoticeExamples.SUCCESS_NOTICE_COMMENT_CREATE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공지/부모 댓글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "notice_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_NOT_FOUND),
                                    @ExampleObject(name = "notice_comment_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_COMMENT_NOT_FOUND)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "bean_validation", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "공지 댓글 작성 요청",
            content = @Content(
                    schema = @Schema(implementation = CreateNoticeCommentRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "comment",
                                    value = """
                                            {
                                              "content": "댓글 내용",
                                              "isAnonymous": false,
                                              "parentId": null
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "reply",
                                    value = """
                                            {
                                              "content": "n단계 답글 내용",
                                              "isAnonymous": true,
                                              "parentId": "notice_comment_uuid"
                                            }
                                            """
                            )
                    }
            )
    )
    public ResponseEntity<ApiResponse<NoticeCommentResponse>> createComment(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "공지 ID", example = "bm90aWNlLTE")
            @PathVariable String noticeId,
            @Valid @RequestBody CreateNoticeCommentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                noticeService.createComment(requireAuthenticatedMember(authenticatedMember).uid(), noticeId, request)
        ));
    }
}
