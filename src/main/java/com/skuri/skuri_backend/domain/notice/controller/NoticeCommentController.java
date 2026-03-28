package com.skuri.skuri_backend.domain.notice.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.notice.dto.request.UpdateNoticeCommentRequest;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentLikeResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/notice-comments")
@Tag(name = "Notice API", description = "학교 공지 조회/댓글/좋아요/읽음 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class NoticeCommentController {

    private final NoticeService noticeService;

    @PatchMapping("/{commentId}")
    @Operation(summary = "공지 댓글 수정", description = "공지 댓글 작성자만 본문을 수정할 수 있습니다. 익명 여부는 생성 시점 값을 유지하며 수정할 수 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeCommentApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNoticeExamples.SUCCESS_NOTICE_COMMENT_UPDATE)
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
                    responseCode = "403",
                    description = "작성자 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_notice_comment_author", value = OpenApiNoticeExamples.ERROR_NOT_NOTICE_COMMENT_AUTHOR)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공지 댓글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "notice_comment_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_COMMENT_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 삭제된 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "comment_already_deleted", value = OpenApiNoticeExamples.ERROR_NOTICE_COMMENT_ALREADY_DELETED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "공지 댓글 수정 요청",
            content = @Content(
                    schema = @Schema(implementation = UpdateNoticeCommentRequest.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "content": "수정된 공지 댓글 내용"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<NoticeCommentResponse>> updateComment(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "공지 댓글 ID", example = "notice_comment_uuid")
            @PathVariable String commentId,
            @Valid @RequestBody UpdateNoticeCommentRequest request
    ) {
        NoticeCommentResponse response = noticeService.updateComment(requireAuthenticatedMember(authenticatedMember).uid(), commentId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{commentId}/like")
    @Operation(summary = "공지 댓글 좋아요", description = "공지 댓글 좋아요를 등록합니다. 이미 좋아요한 상태면 현재 상태를 그대로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "좋아요 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeCommentLikeApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNoticeExamples.SUCCESS_NOTICE_COMMENT_LIKE)
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
                    description = "공지 댓글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "notice_comment_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_COMMENT_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 삭제된 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "comment_already_deleted", value = OpenApiNoticeExamples.ERROR_NOTICE_COMMENT_ALREADY_DELETED)
                    )
            )
    })
    public ResponseEntity<ApiResponse<NoticeCommentLikeResponse>> likeComment(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "공지 댓글 ID", example = "notice_comment_uuid")
            @PathVariable String commentId
    ) {
        NoticeCommentLikeResponse response = noticeService.likeComment(requireAuthenticatedMember(authenticatedMember).uid(), commentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{commentId}/like")
    @Operation(summary = "공지 댓글 좋아요 취소", description = "공지 댓글 좋아요를 취소합니다. 좋아요하지 않은 상태여도 현재 상태를 그대로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "좋아요 취소 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeCommentLikeApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNoticeExamples.SUCCESS_NOTICE_COMMENT_UNLIKE)
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
                    description = "공지 댓글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "notice_comment_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_COMMENT_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 삭제된 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "comment_already_deleted", value = OpenApiNoticeExamples.ERROR_NOTICE_COMMENT_ALREADY_DELETED)
                    )
            )
    })
    public ResponseEntity<ApiResponse<NoticeCommentLikeResponse>> unlikeComment(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "공지 댓글 ID", example = "notice_comment_uuid")
            @PathVariable String commentId
    ) {
        NoticeCommentLikeResponse response = noticeService.unlikeComment(requireAuthenticatedMember(authenticatedMember).uid(), commentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "공지 댓글 삭제", description = "공지 댓글 작성자만 댓글을 삭제할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.SUCCESS_NULL)
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
                    responseCode = "403",
                    description = "작성자 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_notice_comment_author", value = OpenApiNoticeExamples.ERROR_NOT_NOTICE_COMMENT_AUTHOR)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공지 댓글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "notice_comment_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_COMMENT_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 삭제된 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "comment_already_deleted", value = OpenApiNoticeExamples.ERROR_NOTICE_COMMENT_ALREADY_DELETED)
                    )
            )
    })
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "공지 댓글 ID", example = "notice_comment_uuid")
            @PathVariable String commentId
    ) {
        noticeService.deleteComment(requireAuthenticatedMember(authenticatedMember).uid(), commentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
