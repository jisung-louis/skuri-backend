package com.skuri.skuri_backend.domain.board.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.domain.board.dto.request.CreateCommentRequest;
import com.skuri.skuri_backend.domain.board.dto.request.CreatePostRequest;
import com.skuri.skuri_backend.domain.board.dto.request.UpdatePostRequest;
import com.skuri.skuri_backend.domain.board.dto.response.CommentResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostBookmarkResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostDetailResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostLikeResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostSummaryResponse;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/v1/posts")
@Tag(name = "Board API", description = "게시글/댓글/상호작용 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class PostController {

    private final BoardService boardService;

    @PostMapping
    @Operation(summary = "게시글 작성", description = "게시글을 작성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "작성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.PostDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_POST_CREATE)
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
            description = "게시글 작성 요청",
            content = @Content(
                    schema = @Schema(implementation = CreatePostRequest.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "title": "게시글 제목",
                                      "content": "게시글 내용",
                                      "category": "GENERAL",
                                      "isAnonymous": false,
                                      "images": []
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<PostDetailResponse>> createPost(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody CreatePostRequest request
    ) {
        PostDetailResponse response = boardService.createPost(requireAuthenticatedMember(authenticatedMember).uid(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "게시글 목록 조회", description = "카테고리/검색/작성자/정렬 조건으로 게시글 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.PostSummaryPageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_POST_LIST_PAGE)
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
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> getPosts(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestParam(name = "category", required = false) PostCategory category,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "authorId", required = false) String authorId,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size
    ) {
        PageResponse<PostSummaryResponse> response = boardService.getPosts(
                requireAuthenticatedMember(authenticatedMember).uid(),
                category,
                search,
                authorId,
                sort,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{postId}")
    @Operation(summary = "게시글 상세 조회", description = "게시글 상세를 조회하며 조회수를 1 증가시킵니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.PostDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_POST_DETAIL)
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
                    description = "게시글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "post_not_found", value = OpenApiBoardExamples.ERROR_POST_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("postId") String postId
    ) {
        PostDetailResponse response = boardService.getPostDetail(requireAuthenticatedMember(authenticatedMember).uid(), postId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{postId}")
    @Operation(
            summary = "게시글 수정",
            description = "게시글 작성자만 게시글을 수정할 수 있습니다. `images` 필드를 전달하면 전체 이미지 목록을 교체하고, 빈 배열이면 이미지를 모두 제거합니다. `images`를 생략하거나 null로 보내면 기존 이미지를 유지합니다. `isAnonymous`를 전달하면 게시글 익명 상태도 변경할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.PostDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_POST_DETAIL)
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
                            examples = @ExampleObject(name = "not_post_author", value = OpenApiBoardExamples.ERROR_NOT_POST_AUTHOR)
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
            description = "게시글 수정 요청",
            content = @Content(
                    schema = @Schema(implementation = UpdatePostRequest.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "title": "수정된 제목",
                                      "content": "수정된 내용",
                                      "category": "QUESTION",
                                      "isAnonymous": true,
                                      "images": [
                                        {
                                          "url": "https://cdn.skuri.app/posts/post-1/image-1.jpg",
                                          "thumbUrl": "https://cdn.skuri.app/posts/post-1/image-1-thumb.jpg",
                                          "width": 800,
                                          "height": 600,
                                          "size": 245123,
                                          "mime": "image/jpeg"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<PostDetailResponse>> updatePost(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("postId") String postId,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        PostDetailResponse response = boardService.updatePost(requireAuthenticatedMember(authenticatedMember).uid(), postId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "게시글 삭제", description = "게시글 작성자만 게시글을 삭제할 수 있습니다.")
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
                            examples = @ExampleObject(name = "not_post_author", value = OpenApiBoardExamples.ERROR_NOT_POST_AUTHOR)
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
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("postId") String postId
    ) {
        boardService.deletePost(requireAuthenticatedMember(authenticatedMember).uid(), postId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{postId}/like")
    @Operation(summary = "좋아요 등록", description = "게시글 좋아요를 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.PostLikeApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_LIKE)
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
                    description = "게시글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "post_not_found", value = OpenApiBoardExamples.ERROR_POST_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<PostLikeResponse>> likePost(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("postId") String postId
    ) {
        PostLikeResponse response = boardService.likePost(requireAuthenticatedMember(authenticatedMember).uid(), postId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{postId}/like")
    @Operation(summary = "좋아요 취소", description = "게시글 좋아요를 취소합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.PostLikeApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_LIKE)
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
                    description = "게시글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "post_not_found", value = OpenApiBoardExamples.ERROR_POST_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<PostLikeResponse>> unlikePost(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("postId") String postId
    ) {
        PostLikeResponse response = boardService.unlikePost(requireAuthenticatedMember(authenticatedMember).uid(), postId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{postId}/bookmark")
    @Operation(summary = "북마크 등록", description = "게시글 북마크를 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.PostBookmarkApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_BOOKMARK)
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
                    description = "게시글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "post_not_found", value = OpenApiBoardExamples.ERROR_POST_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<PostBookmarkResponse>> bookmarkPost(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("postId") String postId
    ) {
        PostBookmarkResponse response = boardService.bookmarkPost(requireAuthenticatedMember(authenticatedMember).uid(), postId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{postId}/bookmark")
    @Operation(summary = "북마크 취소", description = "게시글 북마크를 취소합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.PostBookmarkApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_BOOKMARK)
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
                    description = "게시글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "post_not_found", value = OpenApiBoardExamples.ERROR_POST_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<PostBookmarkResponse>> unbookmarkPost(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("postId") String postId
    ) {
        PostBookmarkResponse response = boardService.unbookmarkPost(requireAuthenticatedMember(authenticatedMember).uid(), postId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/bookmarked")
    @Operation(summary = "북마크한 게시글 목록", description = "현재 사용자가 북마크한 게시글 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.PostSummaryPageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_POST_LIST_PAGE)
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
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> getBookmarkedPosts(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size
    ) {
        PageResponse<PostSummaryResponse> response = boardService.getBookmarkedPosts(
                requireAuthenticatedMember(authenticatedMember).uid(),
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{postId}/comments")
    @Operation(summary = "댓글 목록 조회", description = "댓글 목록을 flat list로 조회합니다. (무제한 depth)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.CommentListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_COMMENTS_LIST)
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
                    description = "게시글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "post_not_found", value = OpenApiBoardExamples.ERROR_POST_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("postId") String postId
    ) {
        List<CommentResponse> response = boardService.getComments(requireAuthenticatedMember(authenticatedMember).uid(), postId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{postId}/comments")
    @Operation(summary = "댓글 작성", description = "댓글/답글을 작성합니다. depth 제한은 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "작성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiBoardSchemas.CommentApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiBoardExamples.SUCCESS_COMMENT_CREATE)
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
                    description = "게시글/부모 댓글 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "post_not_found", value = OpenApiBoardExamples.ERROR_POST_NOT_FOUND),
                                    @ExampleObject(name = "comment_not_found", value = OpenApiBoardExamples.ERROR_COMMENT_NOT_FOUND)
                            }
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
            description = "댓글 작성 요청",
            content = @Content(
                    schema = @Schema(implementation = CreateCommentRequest.class),
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
                                              "parentId": "parent_comment_uuid"
                                            }
                                            """
                            )
                    }
            )
    )
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("postId") String postId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        CommentResponse response = boardService.createComment(requireAuthenticatedMember(authenticatedMember).uid(), postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
