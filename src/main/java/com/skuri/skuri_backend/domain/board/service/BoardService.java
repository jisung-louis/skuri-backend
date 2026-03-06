package com.skuri.skuri_backend.domain.board.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.board.dto.request.CreateCommentRequest;
import com.skuri.skuri_backend.domain.board.dto.request.CreatePostImageRequest;
import com.skuri.skuri_backend.domain.board.dto.request.CreatePostRequest;
import com.skuri.skuri_backend.domain.board.dto.request.UpdateCommentRequest;
import com.skuri.skuri_backend.domain.board.dto.request.UpdatePostRequest;
import com.skuri.skuri_backend.domain.board.dto.response.CommentResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostBookmarkResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostDetailResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostImageResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostLikeResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostSummaryResponse;
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.entity.PostImage;
import com.skuri.skuri_backend.domain.board.entity.PostInteraction;
import com.skuri.skuri_backend.domain.board.exception.CommentNotFoundException;
import com.skuri.skuri_backend.domain.board.exception.PostNotFoundException;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostInteractionRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.board.repository.PostSummaryProjection;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BoardService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostInteractionRepository postInteractionRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public PostDetailResponse createPost(String memberId, CreatePostRequest request) {
        Member author = findMemberOrThrow(memberId);
        Post post = Post.create(
                request.title().trim(),
                request.content().trim(),
                author.getId(),
                author.getNickname(),
                author.getPhotoUrl(),
                request.isAnonymous(),
                request.category()
        );

        List<CreatePostImageRequest> images = request.images() == null ? List.of() : request.images();
        for (int index = 0; index < images.size(); index++) {
            CreatePostImageRequest image = images.get(index);
            post.appendImage(
                    image.url(),
                    image.thumbUrl(),
                    image.width(),
                    image.height(),
                    image.size(),
                    image.mime(),
                    index
            );
        }

        Post saved = postRepository.save(post);
        saved.assignAnonId();

        return toPostDetailResponse(saved, memberId, false, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getPosts(
            String memberId,
            PostCategory category,
            String search,
            String authorId,
            String sort,
            Integer page,
            Integer size
    ) {
        Pageable pageable = resolvePageable(page, size, sort);
        Page<PostSummaryResponse> postPage = postRepository.searchSummaries(
                        category,
                        trimToNull(search),
                        trimToNull(authorId),
                        pageable
                )
                .map(this::toPostSummaryResponse);

        return PageResponse.from(postPage);
    }

    @Transactional
    public PostDetailResponse getPostDetail(String memberId, String postId) {
        int updatedRows = postRepository.incrementViewCount(postId);
        if (updatedRows == 0) {
            throw new PostNotFoundException();
        }

        Post post = postRepository.findActiveDetailById(postId)
                .orElseThrow(PostNotFoundException::new);

        boolean isLiked = postInteractionRepository.existsById_UserIdAndId_PostIdAndLikedTrue(memberId, postId);
        boolean isBookmarked = postInteractionRepository.existsById_UserIdAndId_PostIdAndBookmarkedTrue(memberId, postId);

        return toPostDetailResponse(post, memberId, isLiked, isBookmarked);
    }

    @Transactional
    public PostDetailResponse updatePost(String memberId, String postId, UpdatePostRequest request) {
        if (request.title() == null && request.content() == null && request.category() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "수정할 필드를 최소 1개 이상 입력해야 합니다.");
        }

        Post post = findActivePostOrThrow(postId);
        requirePostAuthor(post, memberId);

        post.update(
                trimToNull(request.title()),
                trimToNull(request.content()),
                request.category()
        );

        boolean isLiked = postInteractionRepository.existsById_UserIdAndId_PostIdAndLikedTrue(memberId, postId);
        boolean isBookmarked = postInteractionRepository.existsById_UserIdAndId_PostIdAndBookmarkedTrue(memberId, postId);

        return toPostDetailResponse(post, memberId, isLiked, isBookmarked);
    }

    @Transactional
    public void deletePost(String memberId, String postId) {
        Post post = findActivePostOrThrow(postId);
        requirePostAuthor(post, memberId);
        post.markDeleted();
    }

    @Transactional
    public PostLikeResponse likePost(String memberId, String postId) {
        Post post = findActivePostForUpdateOrThrow(postId);
        PostInteraction interaction = getOrCreateInteraction(post, memberId);

        if (interaction.like()) {
            post.increaseLikeCount(1);
        }

        return new PostLikeResponse(interaction.isLiked(), post.getLikeCount());
    }

    @Transactional
    public PostLikeResponse unlikePost(String memberId, String postId) {
        Post post = findActivePostForUpdateOrThrow(postId);
        PostInteraction interaction = getOrCreateInteraction(post, memberId);

        if (interaction.unlike()) {
            post.increaseLikeCount(-1);
        }

        return new PostLikeResponse(interaction.isLiked(), post.getLikeCount());
    }

    @Transactional
    public PostBookmarkResponse bookmarkPost(String memberId, String postId) {
        Post post = findActivePostForUpdateOrThrow(postId);
        PostInteraction interaction = getOrCreateInteraction(post, memberId);

        if (interaction.bookmark()) {
            post.increaseBookmarkCount(1);
        }

        return new PostBookmarkResponse(interaction.isBookmarked(), post.getBookmarkCount());
    }

    @Transactional
    public PostBookmarkResponse unbookmarkPost(String memberId, String postId) {
        Post post = findActivePostForUpdateOrThrow(postId);
        PostInteraction interaction = getOrCreateInteraction(post, memberId);

        if (interaction.unbookmark()) {
            post.increaseBookmarkCount(-1);
        }

        return new PostBookmarkResponse(interaction.isBookmarked(), post.getBookmarkCount());
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getBookmarkedPosts(String memberId, Integer page, Integer size) {
        Pageable pageable = resolvePageable(page, size, "latest");
        Page<PostSummaryResponse> postPage = postRepository.findBookmarkedSummaries(memberId, pageable)
                .map(this::toPostSummaryResponse);

        return PageResponse.from(postPage);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(String memberId, String postId) {
        Post post = findActivePostOrThrow(postId);
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        Map<String, List<Comment>> repliesByParent = new LinkedHashMap<>();
        for (Comment comment : comments) {
            if (comment.hasParent()) {
                repliesByParent.computeIfAbsent(comment.getParent().getId(), key -> new java.util.ArrayList<>()).add(comment);
            }
        }

        return comments.stream()
                .filter(Comment::isRoot)
                .map(root -> {
                    List<CommentResponse> replies = repliesByParent.getOrDefault(root.getId(), List.of()).stream()
                            .map(reply -> toCommentResponse(reply, memberId, post.getAuthorId(), List.of()))
                            .toList();
                    return toCommentResponse(root, memberId, post.getAuthorId(), replies);
                })
                .toList();
    }

    @Transactional
    public CommentResponse createComment(String memberId, String postId, CreateCommentRequest request) {
        Post post = findActivePostForUpdateOrThrow(postId);
        Member author = findMemberOrThrow(memberId);

        Comment parent = null;
        if (request.parentId() != null) {
            parent = commentRepository.findByIdAndPostId(request.parentId(), postId)
                    .orElseThrow(CommentNotFoundException::new);
            if (parent.hasParent()) {
                throw new BusinessException(ErrorCode.COMMENT_DEPTH_EXCEEDED);
            }
        }

        AnonymousMetadata anonymousMetadata = resolveAnonymousMetadata(postId, memberId, request.isAnonymous());
        Comment comment = Comment.create(
                post,
                request.content().trim(),
                author.getId(),
                author.getNickname(),
                author.getPhotoUrl(),
                request.isAnonymous(),
                anonymousMetadata.anonId,
                anonymousMetadata.anonymousOrder,
                parent
        );
        Comment saved = commentRepository.save(comment);

        post.increaseCommentCount(1);
        post.updateLastCommentAt(LocalDateTime.now());

        return toCommentResponse(saved, memberId, post.getAuthorId(), List.of());
    }

    @Transactional
    public CommentResponse updateComment(String memberId, String commentId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findActiveById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        requireCommentAuthor(comment, memberId);
        if (comment.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_ALREADY_DELETED);
        }

        comment.updateContent(request.content().trim());
        return toCommentResponse(comment, memberId, comment.getPost().getAuthorId(), List.of());
    }

    @Transactional
    public void deleteComment(String memberId, String commentId) {
        Comment comment = commentRepository.findActiveById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        requireCommentAuthor(comment, memberId);
        if (comment.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_ALREADY_DELETED);
        }

        Post post = findActivePostForUpdateOrThrow(comment.getPost().getId());
        comment.softDelete();
        post.increaseCommentCount(-1);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getMyPosts(String memberId, Integer page, Integer size) {
        Pageable pageable = resolvePageable(page, size, "latest");
        Page<PostSummaryResponse> postPage = postRepository.findActiveSummariesByAuthorId(memberId, pageable)
                .map(this::toPostSummaryResponse);

        return PageResponse.from(postPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getMyBookmarks(String memberId, Integer page, Integer size) {
        return getBookmarkedPosts(memberId, page, size);
    }

    private Pageable resolvePageable(Integer page, Integer size, String sort) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? DEFAULT_PAGE_SIZE : size;

        if (resolvedPage < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page는 0 이상이어야 합니다.");
        }
        if (resolvedSize < 1 || resolvedSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1 이상 100 이하여야 합니다.");
        }

        return PageRequest.of(resolvedPage, resolvedSize, resolveSort(sort));
    }

    private Sort resolveSort(String sort) {
        String normalized = trimToNull(sort);
        if (normalized == null || "latest".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createdAt"));
        }
        if ("popular".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("createdAt"));
        }
        if ("mostCommented".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Order.desc("commentCount"), Sort.Order.desc("createdAt"));
        }
        if ("mostViewed".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("createdAt"));
        }

        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 sort 값입니다: " + normalized);
    }

    private PostSummaryResponse toPostSummaryResponse(PostSummaryProjection post) {
        AuthorView authorView = resolveAuthorView(post.isAnonymous(), false, post.getAuthorId(), post.getAuthorName(), post.getAuthorProfileImage(), null);

        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                toPreview(post.getContent()),
                authorView.authorId,
                authorView.authorName,
                authorView.authorProfileImage,
                post.isAnonymous(),
                post.getCategory(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.isHasImage(),
                post.isPinned(),
                post.getCreatedAt()
        );
    }

    private PostDetailResponse toPostDetailResponse(Post post, String memberId, boolean isLiked, boolean isBookmarked) {
        AuthorView authorView = resolveAuthorView(post.isAnonymous(), post.isDeleted(), post.getAuthorId(), post.getAuthorName(), post.getAuthorProfileImage(), null);

        List<PostImageResponse> images = post.getImages().stream()
                .map(this::toPostImageResponse)
                .toList();

        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                authorView.authorId,
                authorView.authorName,
                authorView.authorProfileImage,
                post.isAnonymous(),
                post.getCategory(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getBookmarkCount(),
                images,
                isLiked,
                isBookmarked,
                post.isAuthor(memberId),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
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

    private CommentResponse toCommentResponse(
            Comment comment,
            String memberId,
            String postAuthorId,
            List<CommentResponse> replies
    ) {
        boolean deleted = comment.isDeleted();
        AuthorView authorView = resolveAuthorView(
                comment.isAnonymous(),
                deleted,
                comment.getAuthorId(),
                comment.getAuthorName(),
                comment.getAuthorProfileImage(),
                comment.getAnonymousOrder()
        );

        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                authorView.authorId,
                authorView.authorName,
                authorView.authorProfileImage,
                !deleted && comment.isAnonymous(),
                deleted ? null : comment.getAnonymousOrder(),
                !deleted && comment.isAuthor(memberId),
                !deleted && comment.isAuthor(postAuthorId),
                deleted,
                replies,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private AuthorView resolveAuthorView(
            boolean anonymous,
            boolean deleted,
            String authorId,
            String authorName,
            String authorProfileImage,
            Integer anonymousOrder
    ) {
        if (deleted) {
            return new AuthorView(null, null, null);
        }

        if (!anonymous) {
            return new AuthorView(authorId, authorName, authorProfileImage);
        }

        String displayName = anonymousOrder == null ? "익명" : "익명" + anonymousOrder;
        return new AuthorView(null, displayName, null);
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

    private Post findActivePostOrThrow(String postId) {
        return postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    private Post findActivePostForUpdateOrThrow(String postId) {
        return postRepository.findActiveByIdForUpdate(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    private Member findMemberOrThrow(String memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private void requirePostAuthor(Post post, String actorId) {
        if (!post.isAuthor(actorId)) {
            throw new BusinessException(ErrorCode.NOT_POST_AUTHOR);
        }
    }

    private void requireCommentAuthor(Comment comment, String actorId) {
        if (!comment.isAuthor(actorId)) {
            throw new BusinessException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
    }

    private PostInteraction getOrCreateInteraction(Post post, String userId) {
        return postInteractionRepository.findById_UserIdAndId_PostId(userId, post.getId())
                .orElseGet(() -> postInteractionRepository.save(PostInteraction.create(post, userId)));
    }

    private AnonymousMetadata resolveAnonymousMetadata(String postId, String userId, boolean anonymous) {
        if (!anonymous) {
            return new AnonymousMetadata(null, null);
        }

        String anonId = postId + ":" + userId;
        Integer existingOrder = commentRepository
                .findFirstByPost_IdAndAnonIdAndAnonymousOrderIsNotNullOrderByCreatedAtAsc(postId, anonId)
                .map(Comment::getAnonymousOrder)
                .orElse(null);

        if (existingOrder != null) {
            return new AnonymousMetadata(anonId, existingOrder);
        }

        int nextOrder = commentRepository.findMaxAnonymousOrderByPostId(postId) + 1;
        return new AnonymousMetadata(anonId, nextOrder);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class AnonymousMetadata {
        private final String anonId;
        private final Integer anonymousOrder;

        private AnonymousMetadata(String anonId, Integer anonymousOrder) {
            this.anonId = anonId;
            this.anonymousOrder = anonymousOrder;
        }
    }

    private static final class AuthorView {
        private final String authorId;
        private final String authorName;
        private final String authorProfileImage;

        private AuthorView(String authorId, String authorName, String authorProfileImage) {
            this.authorId = authorId;
            this.authorName = authorName;
            this.authorProfileImage = authorProfileImage;
        }
    }
}
