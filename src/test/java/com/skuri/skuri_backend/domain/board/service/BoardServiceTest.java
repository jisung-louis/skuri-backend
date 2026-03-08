package com.skuri.skuri_backend.domain.board.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.board.dto.request.CreateCommentRequest;
import com.skuri.skuri_backend.domain.board.dto.request.CreatePostImageRequest;
import com.skuri.skuri_backend.domain.board.dto.request.CreatePostRequest;
import com.skuri.skuri_backend.domain.board.dto.request.UpdatePostRequest;
import com.skuri.skuri_backend.domain.board.dto.response.CommentResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostDetailResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostBookmarkResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostLikeResponse;
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.entity.PostInteraction;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostInteractionRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostInteractionRepository postInteractionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BoardService boardService;

    @Test
    void createPost_정상생성() {
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "사용자", LocalDateTime.now());
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(member));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "post-1");
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });

        PostDetailResponse response = boardService.createPost(
                "member-1",
                new CreatePostRequest(
                        "게시글 제목",
                        "게시글 내용",
                        PostCategory.GENERAL,
                        true,
                        List.of(new CreatePostImageRequest("https://example.com/image.jpg", null, 800, 600, 12345, "image/jpeg"))
                )
        );

        assertEquals("post-1", response.id());
        assertTrue(response.isAnonymous());
        assertEquals("익명", response.authorName());
    }

    @Test
    void createComment_대댓글은_무제한으로허용된다() {
        Post post = post("post-1", "author-1");
        Comment parent = comment("comment-1", post, null, "author-1", false, null);
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "사용자", LocalDateTime.now());

        when(postRepository.findActiveByIdForUpdate("post-1")).thenReturn(Optional.of(post));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(member));
        when(commentRepository.findByIdAndPostId("comment-1", "post-1")).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "comment-2");
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });

        CommentResponse response = boardService.createComment(
                "member-1",
                "post-1",
                new CreateCommentRequest("대댓글", false, "comment-1")
        );

        assertEquals("comment-2", response.id());
        assertEquals("comment-1", response.parentId());
        assertEquals(1, response.depth());
        assertEquals(1, post.getCommentCount());
    }

    @Test
    void createComment_대대댓글도허용된다() {
        Post post = post("post-1", "author-1");
        Comment root = comment("root-1", post, null, "author-1", false, null);
        Comment child = comment("child-1", post, root, "member-2", false, null);
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "사용자", LocalDateTime.now());

        when(postRepository.findActiveByIdForUpdate("post-1")).thenReturn(Optional.of(post));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(member));
        when(commentRepository.findByIdAndPostId("child-1", "post-1")).thenReturn(Optional.of(child));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "comment-3");
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });

        CommentResponse response = boardService.createComment(
                "member-1",
                "post-1",
                new CreateCommentRequest("대대댓글", false, "child-1")
        );

        assertEquals("comment-3", response.id());
        assertEquals("child-1", response.parentId());
        assertEquals(2, response.depth());
    }

    @Test
    void getComments_flatList로반환되고_부모삭제시_placeholder유지_자손은유지된다() {
        Post post = post("post-1", "author-1");
        Comment parent = comment("comment-1", post, null, "author-1", false, null);
        Comment child = comment("comment-2", post, parent, "member-2", true, 2);
        Comment grandChild = comment("comment-3", post, child, "member-3", false, null);
        parent.softDelete();

        when(postRepository.findByIdAndDeletedFalse("post-1")).thenReturn(Optional.of(post));
        when(commentRepository.findByPostIdOrderByCreatedAtAsc("post-1")).thenReturn(List.of(parent, child, grandChild));

        List<CommentResponse> responses = boardService.getComments("member-3", "post-1");

        assertEquals(3, responses.size());
        assertTrue(responses.get(0).isDeleted());
        assertEquals(Comment.DELETED_PLACEHOLDER, responses.get(0).content());
        assertEquals(0, responses.get(0).depth());
        assertEquals("comment-2", responses.get(1).id());
        assertEquals("comment-1", responses.get(1).parentId());
        assertEquals(1, responses.get(1).depth());
        assertEquals("comment-3", responses.get(2).id());
        assertEquals("comment-2", responses.get(2).parentId());
        assertEquals(2, responses.get(2).depth());
    }

    @Test
    void createComment_익명순번은_기존순번을재사용한다() {
        Post post = post("post-1", "author-1");
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "사용자", LocalDateTime.now());
        Comment existingAnonymous = comment("old-comment", post, null, "member-1", true, 2);

        when(postRepository.findActiveByIdForUpdate("post-1")).thenReturn(Optional.of(post));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(member));
        when(commentRepository.findFirstByPost_IdAndAnonIdAndAnonymousOrderIsNotNullOrderByCreatedAtAsc("post-1", "post-1:member-1"))
                .thenReturn(Optional.of(existingAnonymous));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "comment-new");
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });

        boardService.createComment(
                "member-1",
                "post-1",
                new CreateCommentRequest("익명 댓글", true, null)
        );

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertEquals("post-1:member-1", captor.getValue().getAnonId());
        assertEquals(2, captor.getValue().getAnonymousOrder());
    }

    @Test
    void likeUnlike_카운트가동기화된다() {
        Post post = post("post-1", "author-1");
        PostInteraction interaction = PostInteraction.create(post, "member-1");

        when(postRepository.findActiveByIdForUpdate("post-1")).thenReturn(Optional.of(post));
        when(postInteractionRepository.findById_UserIdAndId_PostId("member-1", "post-1"))
                .thenReturn(Optional.of(interaction));

        PostLikeResponse liked = boardService.likePost("member-1", "post-1");
        PostLikeResponse unliked = boardService.unlikePost("member-1", "post-1");

        assertTrue(liked.isLiked());
        assertEquals(1, liked.likeCount());
        assertFalse(unliked.isLiked());
        assertEquals(0, unliked.likeCount());
    }

    @Test
    void bookmarkUnbookmark_카운트가동기화된다() {
        Post post = post("post-1", "author-1");
        PostInteraction interaction = PostInteraction.create(post, "member-1");

        when(postRepository.findActiveByIdForUpdate("post-1")).thenReturn(Optional.of(post));
        when(postInteractionRepository.findById_UserIdAndId_PostId("member-1", "post-1"))
                .thenReturn(Optional.of(interaction));

        PostBookmarkResponse bookmarked = boardService.bookmarkPost("member-1", "post-1");
        PostBookmarkResponse unbookmarked = boardService.unbookmarkPost("member-1", "post-1");

        assertTrue(bookmarked.isBookmarked());
        assertEquals(1, bookmarked.bookmarkCount());
        assertFalse(unbookmarked.isBookmarked());
        assertEquals(0, unbookmarked.bookmarkCount());
    }

    @Test
    void updatePost_작성자위반이면_예외() {
        Post post = post("post-1", "author-1");
        when(postRepository.findByIdAndDeletedFalse("post-1")).thenReturn(Optional.of(post));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> boardService.updatePost("member-2", "post-1", new UpdatePostRequest("수정", null, null))
        );

        assertEquals(ErrorCode.NOT_POST_AUTHOR, exception.getErrorCode());
    }

    @Test
    void updatePost_공백만전달되면_VALIDATION_ERROR() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> boardService.updatePost("member-1", "post-1", new UpdatePostRequest("   ", null, null))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(postRepository, never()).findByIdAndDeletedFalse("post-1");
    }

    @Test
    void deletePost_작성자이면_softDelete() {
        Post post = post("post-1", "author-1");
        when(postRepository.findByIdAndDeletedFalse("post-1")).thenReturn(Optional.of(post));

        boardService.deletePost("author-1", "post-1");

        assertTrue(post.isDeleted());
    }

    @Test
    void deleteComment_정상삭제시_placeholder와카운트동기화() {
        Post post = post("post-1", "author-1");
        ReflectionTestUtils.setField(post, "commentCount", 2);
        Comment comment = comment("comment-1", post, null, "member-1", false, null);

        when(commentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(comment));
        when(postRepository.findActiveByIdForUpdate("post-1")).thenReturn(Optional.of(post));

        boardService.deleteComment("member-1", "comment-1");

        assertTrue(comment.isDeleted());
        assertEquals(Comment.DELETED_PLACEHOLDER, comment.getContent());
        assertEquals(1, post.getCommentCount());
    }

    @Test
    void deleteComment_이미삭제된댓글이면_COMMENT_ALREADY_DELETED() {
        Post post = post("post-1", "author-1");
        ReflectionTestUtils.setField(post, "commentCount", 2);
        Comment comment = comment("comment-1", post, null, "member-1", false, null);
        comment.softDelete();

        when(commentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(comment));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> boardService.deleteComment("member-1", "comment-1")
        );

        assertEquals(ErrorCode.COMMENT_ALREADY_DELETED, exception.getErrorCode());
        verify(postRepository, never()).findActiveByIdForUpdate("post-1");
    }

    private Post post(String id, String authorId) {
        Post post = Post.create("제목", "본문", authorId, "작성자", "https://example.com/profile.jpg", false, PostCategory.GENERAL);
        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(post, "updatedAt", LocalDateTime.now());
        return post;
    }

    private Comment comment(
            String id,
            Post post,
            Comment parent,
            String authorId,
            boolean anonymous,
            Integer anonymousOrder
    ) {
        Comment comment = Comment.create(
                post,
                "댓글",
                authorId,
                "작성자",
                "https://example.com/profile.jpg",
                anonymous,
                anonymous ? post.getId() + ":" + authorId : null,
                anonymousOrder,
                parent
        );
        ReflectionTestUtils.setField(comment, "id", id);
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(comment, "updatedAt", LocalDateTime.now());
        return comment;
    }
}
