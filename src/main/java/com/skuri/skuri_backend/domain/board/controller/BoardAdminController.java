package com.skuri.skuri_backend.domain.board.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.domain.board.dto.request.UpdateBoardModerationRequest;
import com.skuri.skuri_backend.domain.board.dto.response.AdminCommentSummaryResponse;
import com.skuri.skuri_backend.domain.board.dto.response.AdminPostDetailResponse;
import com.skuri.skuri_backend.domain.board.dto.response.AdminPostSummaryResponse;
import com.skuri.skuri_backend.domain.board.dto.response.BoardModerationResponse;
import com.skuri.skuri_backend.domain.board.service.BoardAdminService;
import com.skuri.skuri_backend.infra.admin.audit.AdminAudit;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditActions;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditTargetTypes;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import com.skuri.skuri_backend.infra.openapi.OpenApiBoardExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiBoardSchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin")
@Tag(name = "Board Admin API", description = "관리자 게시글/댓글 운영 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AdminApiAccess
public class BoardAdminController {

    private final BoardAdminService boardAdminService;

    @GetMapping("/posts")
    @Operation(
            summary = "게시글 목록 조회(관리자)",
            description = "관리자용 게시글 목록을 검색/필터/페이지네이션으로 조회합니다. 기본 정렬은 createdAt DESC입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.AdminPostSummaryPageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_ADMIN_POST_LIST_PAGE)
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
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "페이지네이션 또는 필터 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION),
                                    @ExampleObject(name = "invalid_category", value = OpenApiBoardExamples.ERROR_ADMIN_POST_CATEGORY_INVALID),
                                    @ExampleObject(name = "invalid_moderation_status", value = OpenApiBoardExamples.ERROR_ADMIN_MODERATION_STATUS_INVALID)
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<PageResponse<AdminPostSummaryResponse>>> getAdminPosts(
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size,
            @Parameter(description = "제목/본문/작성자 검색어", example = "택시")
            @RequestParam(name = "query", required = false) String query,
            @Parameter(
                    description = "카테고리 필터",
                    example = "GENERAL",
                    schema = @Schema(allowableValues = {"GENERAL", "QUESTION", "REVIEW", "ANNOUNCEMENT"})
            )
            @RequestParam(name = "category", required = false) String category,
            @Parameter(
                    description = "moderation 상태 필터",
                    example = "VISIBLE",
                    schema = @Schema(allowableValues = {"VISIBLE", "HIDDEN", "DELETED"})
            )
            @RequestParam(name = "moderationStatus", required = false) String moderationStatus,
            @Parameter(description = "작성자 ID 필터", example = "member-1")
            @RequestParam(name = "authorId", required = false) String authorId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                boardAdminService.getAdminPosts(query, category, moderationStatus, authorId, page, size)
        ));
    }

    @GetMapping("/posts/{postId}")
    @Operation(summary = "게시글 상세 조회(관리자)", description = "관리자용 게시글 상세와 moderation 메타데이터를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.AdminPostDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_ADMIN_POST_DETAIL)
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
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "게시글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "post_not_found", value = OpenApiBoardExamples.ERROR_POST_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<AdminPostDetailResponse>> getAdminPost(
            @Parameter(description = "게시글 ID", example = "post_uuid")
            @PathVariable String postId
    ) {
        return ResponseEntity.ok(ApiResponse.success(boardAdminService.getAdminPost(postId)));
    }

    @PatchMapping("/posts/{postId}/moderation")
    @Operation(summary = "게시글 moderation 상태 변경(관리자)", description = "게시글 moderation 상태를 VISIBLE, HIDDEN, DELETED 중 하나로 변경합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.BoardModerationApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_ADMIN_POST_MODERATION_UPDATE)
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
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "게시글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "post_not_found", value = OpenApiBoardExamples.ERROR_POST_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않는 moderation 상태 전이",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "invalid_post_moderation_transition",
                                    value = OpenApiBoardExamples.ERROR_INVALID_POST_MODERATION_STATUS_TRANSITION
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION),
                                    @ExampleObject(name = "invalid_status", value = OpenApiBoardExamples.ERROR_ADMIN_REQUEST_STATUS_INVALID)
                            }
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "게시글 moderation 상태 변경 요청",
            content = @Content(
                    schema = @Schema(implementation = UpdateBoardModerationRequest.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "status": "HIDDEN"
                                    }
                                    """
                    )
            )
    )
    @AdminAudit(
            action = AdminAuditActions.POST_MODERATION_UPDATED,
            targetType = AdminAuditTargetTypes.POST,
            targetId = "#postId",
            before = "@adminAuditSnapshots.postModeration(#postId)",
            after = "@adminAuditSnapshots.postModeration(#postId)"
    )
    public ResponseEntity<ApiResponse<BoardModerationResponse>> updatePostModeration(
            @Parameter(description = "게시글 ID", example = "post_uuid")
            @PathVariable String postId,
            @Valid @RequestBody UpdateBoardModerationRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(boardAdminService.updatePostModeration(postId, request)));
    }

    @GetMapping("/comments")
    @Operation(
            summary = "댓글 목록 조회(관리자)",
            description = "관리자용 댓글 목록을 검색/필터/페이지네이션으로 조회합니다. 기본 정렬은 createdAt DESC입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.AdminCommentSummaryPageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_ADMIN_COMMENT_LIST_PAGE)
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
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "페이지네이션 또는 필터 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION),
                                    @ExampleObject(name = "invalid_status", value = OpenApiBoardExamples.ERROR_ADMIN_REQUEST_STATUS_INVALID)
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<PageResponse<AdminCommentSummaryResponse>>> getAdminComments(
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size,
            @Parameter(description = "게시글 ID 필터", example = "post_uuid")
            @RequestParam(name = "postId", required = false) String postId,
            @Parameter(description = "댓글/게시글/작성자 검색어", example = "욕설")
            @RequestParam(name = "query", required = false) String query,
            @Parameter(
                    description = "moderation 상태 필터",
                    example = "HIDDEN",
                    schema = @Schema(allowableValues = {"VISIBLE", "HIDDEN", "DELETED"})
            )
            @RequestParam(name = "moderationStatus", required = false) String moderationStatus,
            @Parameter(description = "작성자 ID 필터", example = "member-1")
            @RequestParam(name = "authorId", required = false) String authorId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                boardAdminService.getAdminComments(postId, query, moderationStatus, authorId, page, size)
        ));
    }

    @PatchMapping("/comments/{commentId}/moderation")
    @Operation(summary = "댓글 moderation 상태 변경(관리자)", description = "댓글 moderation 상태를 VISIBLE, HIDDEN, DELETED 중 하나로 변경합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.BoardModerationApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_ADMIN_COMMENT_MODERATION_UPDATE)
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
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "댓글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "comment_not_found", value = OpenApiBoardExamples.ERROR_COMMENT_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않는 moderation 상태 전이",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "invalid_comment_moderation_transition",
                                    value = OpenApiBoardExamples.ERROR_INVALID_COMMENT_MODERATION_STATUS_TRANSITION
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION),
                                    @ExampleObject(name = "invalid_moderation_status", value = OpenApiBoardExamples.ERROR_ADMIN_MODERATION_STATUS_INVALID)
                            }
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "댓글 moderation 상태 변경 요청",
            content = @Content(
                    schema = @Schema(implementation = UpdateBoardModerationRequest.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "status": "DELETED"
                                    }
                                    """
                    )
            )
    )
    @AdminAudit(
            action = AdminAuditActions.COMMENT_MODERATION_UPDATED,
            targetType = AdminAuditTargetTypes.COMMENT,
            targetId = "#commentId",
            before = "@adminAuditSnapshots.commentModeration(#commentId)",
            after = "@adminAuditSnapshots.commentModeration(#commentId)"
    )
    public ResponseEntity<ApiResponse<BoardModerationResponse>> updateCommentModeration(
            @Parameter(description = "댓글 ID", example = "comment_uuid")
            @PathVariable String commentId,
            @Valid @RequestBody UpdateBoardModerationRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(boardAdminService.updateCommentModeration(commentId, request)));
    }
}
