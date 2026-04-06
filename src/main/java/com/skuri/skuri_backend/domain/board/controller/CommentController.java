package com.skuri.skuri_backend.domain.board.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.board.dto.request.UpdateCommentRequest;
import com.skuri.skuri_backend.domain.board.dto.response.CommentLikeResponse;
import com.skuri.skuri_backend.domain.board.dto.response.CommentResponse;
import com.skuri.skuri_backend.domain.board.service.BoardService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
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
@RequestMapping("/v1/comments")
@Tag(name = "Board API", description = "댓글 수정/삭제/좋아요 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class CommentController {

    private final BoardService boardService;

    @PatchMapping("/{commentId}")
    @Operation(summary = "댓글 수정", description = "댓글 작성자만 수정할 수 있습니다. `isAnonymous`를 보내면 익명 여부도 변경할 수 있고, 생략하면 기존 값을 유지합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.CommentApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_COMMENT_UPDATE)
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
                            examples = @ExampleObject(name = "not_comment_author", value = OpenApiBoardExamples.ERROR_NOT_COMMENT_AUTHOR)
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
                    description = "이미 삭제된 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "comment_deleted", value = OpenApiBoardExamples.ERROR_COMMENT_ALREADY_DELETED)
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
            description = "댓글 수정 요청",
            content = @Content(
                    schema = @Schema(implementation = UpdateCommentRequest.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "content": "수정된 댓글 내용",
                                      "isAnonymous": true
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("commentId") String commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        CommentResponse response = boardService.updateComment(requireAuthenticatedMember(authenticatedMember).uid(), commentId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{commentId}/like")
    @Operation(summary = "댓글 좋아요", description = "댓글 좋아요를 등록합니다. 이미 좋아요한 상태면 현재 상태를 그대로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "좋아요 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.CommentLikeApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_COMMENT_LIKE)
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
                    description = "댓글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "comment_not_found", value = OpenApiBoardExamples.ERROR_COMMENT_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 삭제된 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "comment_deleted", value = OpenApiBoardExamples.ERROR_COMMENT_ALREADY_DELETED)
                    )
            )
    })
    public ResponseEntity<ApiResponse<CommentLikeResponse>> likeComment(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("commentId") String commentId
    ) {
        CommentLikeResponse response = boardService.likeComment(requireAuthenticatedMember(authenticatedMember).uid(), commentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{commentId}/like")
    @Operation(summary = "댓글 좋아요 취소", description = "댓글 좋아요를 취소합니다. 좋아요하지 않은 상태여도 현재 상태를 그대로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "좋아요 취소 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.CommentLikeApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_COMMENT_UNLIKE)
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
                    description = "댓글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "comment_not_found", value = OpenApiBoardExamples.ERROR_COMMENT_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 삭제된 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "comment_deleted", value = OpenApiBoardExamples.ERROR_COMMENT_ALREADY_DELETED)
                    )
            )
    })
    public ResponseEntity<ApiResponse<CommentLikeResponse>> unlikeComment(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("commentId") String commentId
    ) {
        CommentLikeResponse response = boardService.unlikeComment(requireAuthenticatedMember(authenticatedMember).uid(), commentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "댓글 삭제", description = "댓글을 placeholder(삭제된 댓글입니다)로 soft delete 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_COMMENT_DELETED)
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
                            examples = @ExampleObject(name = "not_comment_author", value = OpenApiBoardExamples.ERROR_NOT_COMMENT_AUTHOR)
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
                    description = "이미 삭제된 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "comment_deleted", value = OpenApiBoardExamples.ERROR_COMMENT_ALREADY_DELETED)
                    )
            )
    })
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("commentId") String commentId
    ) {
        boardService.deleteComment(requireAuthenticatedMember(authenticatedMember).uid(), commentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
