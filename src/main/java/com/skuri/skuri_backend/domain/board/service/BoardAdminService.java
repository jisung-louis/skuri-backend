package com.skuri.skuri_backend.domain.board.service;

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
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.entity.PostImage;
import com.skuri.skuri_backend.domain.board.exception.CommentNotFoundException;
import com.skuri.skuri_backend.domain.board.exception.PostNotFoundException;
import com.skuri.skuri_backend.domain.board.repository.AdminCommentSummaryProjection;
import com.skuri.skuri_backend.domain.board.repository.AdminPostSummaryProjection;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostImageRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.board.repository.PostThumbnailProjection;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.infra.admin.list.AdminPageRequestPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardAdminService {

    private static final Sort DEFAULT_POST_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final Sort DEFAULT_COMMENT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminPostSummaryResponse> getAdminPosts(
            String query,
            String category,
            String moderationStatus,
            String authorId,
            int page,
            int size
    ) {
        Page<AdminPostSummaryProjection> postPage = postRepository.searchAdminSummaries(
                trimToNull(query),
                parseCategory(category),
                normalizeModerationStatus(moderationStatus),
                trimToNull(authorId),
                pageable(page, size, DEFAULT_POST_SORT)
        );

        List<String> postIds = postPage.getContent().stream()
                .map(AdminPostSummaryProjection::getId)
                .toList();
        Map<String, String> thumbnails = resolveThumbnailUrls(postIds);

        return PageResponse.from(postPage.map(post -> new AdminPostSummaryResponse(
                post.getId(),
                post.getCategory(),
                post.getTitle(),
                post.getAuthorId(),
                post.getAuthorNickname(),
                post.getAuthorRealname(),
                post.isAnonymous(),
                post.getCommentCount(),
                post.getLikeCount(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                BoardModerationStatus.from(post.isHidden(), post.isDeleted()),
                thumbnails.get(post.getId())
        )));
    }

    @Transactional(readOnly = true)
    public AdminPostDetailResponse getAdminPost(String postId) {
        Post post = postRepository.findAdminDetailById(postId)
                .orElseThrow(PostNotFoundException::new);

        Optional<Member> author = memberRepository.findById(post.getAuthorId());
        return new AdminPostDetailResponse(
                post.getId(),
                post.getCategory(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorId(),
                author.map(Member::getNickname).orElse(post.getAuthorName()),
                author.map(Member::getRealname).orElse(null),
                post.isAnonymous(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getBookmarkCount(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                BoardModerationStatus.fromPost(post),
                resolveThumbnailUrl(post.getImages()),
                post.getImages().stream().map(this::toPostImageResponse).toList()
        );
    }

    @Transactional
    public BoardModerationResponse updatePostModeration(String postId, UpdateBoardModerationRequest request) {
        Post post = postRepository.findByIdForAdminUpdate(postId)
                .orElseThrow(PostNotFoundException::new);

        BoardModerationStatus target = BoardModerationStatus.parse(request.status(), "status");
        BoardModerationStatus current = BoardModerationStatus.fromPost(post);

        switch (target) {
            case VISIBLE -> {
                if (current != BoardModerationStatus.HIDDEN) {
                    throw new BusinessException(ErrorCode.INVALID_POST_MODERATION_STATUS_TRANSITION);
                }
                post.unhide();
            }
            case HIDDEN -> {
                if (current != BoardModerationStatus.VISIBLE) {
                    throw new BusinessException(ErrorCode.INVALID_POST_MODERATION_STATUS_TRANSITION);
                }
                post.hide();
            }
            case DELETED -> {
                if (current == BoardModerationStatus.DELETED) {
                    throw new BusinessException(ErrorCode.INVALID_POST_MODERATION_STATUS_TRANSITION);
                }
                post.markDeleted();
            }
        }

        return new BoardModerationResponse(post.getId(), BoardModerationStatus.fromPost(post));
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminCommentSummaryResponse> getAdminComments(
            String postId,
            String query,
            String moderationStatus,
            String authorId,
            int page,
            int size
    ) {
        Page<AdminCommentSummaryProjection> commentPage = commentRepository.searchAdminSummaries(
                trimToNull(postId),
                trimToNull(query),
                normalizeModerationStatus(moderationStatus),
                trimToNull(authorId),
                pageable(page, size, DEFAULT_COMMENT_SORT)
        );

        return PageResponse.from(commentPage.map(comment -> new AdminCommentSummaryResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getPostTitle(),
                comment.getAuthorId(),
                comment.getAuthorNickname(),
                comment.getAuthorRealname(),
                toPreview(comment.getContent()),
                comment.getParentCommentId(),
                comment.getCreatedAt(),
                BoardModerationStatus.from(comment.isHidden(), comment.isDeleted())
        )));
    }

    @Transactional
    public BoardModerationResponse updateCommentModeration(String commentId, UpdateBoardModerationRequest request) {
        Comment comment = commentRepository.findByIdForAdminUpdate(commentId)
                .orElseThrow(CommentNotFoundException::new);

        BoardModerationStatus target = BoardModerationStatus.parse(request.status(), "status");
        BoardModerationStatus current = BoardModerationStatus.fromComment(comment);

        switch (target) {
            case VISIBLE -> {
                if (current != BoardModerationStatus.HIDDEN) {
                    throw new BusinessException(ErrorCode.INVALID_COMMENT_MODERATION_STATUS_TRANSITION);
                }
                comment.unhide();
                comment.getPost().increaseCommentCount(1);
            }
            case HIDDEN -> {
                if (current != BoardModerationStatus.VISIBLE) {
                    throw new BusinessException(ErrorCode.INVALID_COMMENT_MODERATION_STATUS_TRANSITION);
                }
                comment.hide();
                comment.getPost().increaseCommentCount(-1);
            }
            case DELETED -> {
                if (current == BoardModerationStatus.DELETED) {
                    throw new BusinessException(ErrorCode.INVALID_COMMENT_MODERATION_STATUS_TRANSITION);
                }
                if (current == BoardModerationStatus.VISIBLE) {
                    comment.getPost().increaseCommentCount(-1);
                }
                comment.softDelete();
            }
        }

        return new BoardModerationResponse(comment.getId(), BoardModerationStatus.fromComment(comment));
    }

    private Pageable pageable(int page, int size, Sort sort) {
        Pageable validated = AdminPageRequestPolicy.of(page, size);
        return PageRequest.of(validated.getPageNumber(), validated.getPageSize(), sort);
    }

    private PostCategory parseCategory(String rawCategory) {
        String normalized = trimToNull(rawCategory);
        if (normalized == null) {
            return null;
        }

        try {
            return PostCategory.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "category는 GENERAL, QUESTION, REVIEW, ANNOUNCEMENT 중 하나여야 합니다."
            );
        }
    }

    private String normalizeModerationStatus(String rawStatus) {
        BoardModerationStatus status = BoardModerationStatus.parseNullable(rawStatus, "moderationStatus");
        return status == null ? null : status.name();
    }

    private Map<String, String> resolveThumbnailUrls(List<String> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        return postImageRepository.findFirstThumbnailByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        PostThumbnailProjection::getPostId,
                        PostThumbnailProjection::getThumbnailUrl,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private String resolveThumbnailUrl(List<PostImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        PostImage first = images.getFirst();
        if (first.getThumbUrl() == null || first.getThumbUrl().isBlank()) {
            return first.getUrl();
        }
        return first.getThumbUrl();
    }

    private PostImageResponse toPostImageResponse(PostImage image) {
        return new PostImageResponse(
                image.getUrl(),
                image.getThumbUrl(),
                image.getWidth(),
                image.getHeight(),
                image.getSize(),
                image.getMime()
        );
    }

    private String toPreview(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() <= 120) {
            return content;
        }
        return content.substring(0, 117) + "...";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
