package com.skuri.skuri_backend.domain.board.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.board.dto.request.UpdateBoardModerationRequest;
import com.skuri.skuri_backend.domain.board.dto.response.BoardModerationResponse;
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostImageRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardAdminServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private BoardAdminService boardAdminService;

    @Test
    void updatePostModeration_VISIBLE에서_HIDDEN으로_변경한다() {
        Post post = post("post-1", "member-1");
        when(postRepository.findByIdForAdminUpdate("post-1")).thenReturn(Optional.of(post));

        BoardModerationResponse response = boardAdminService.updatePostModeration(
                "post-1",
                new UpdateBoardModerationRequest("HIDDEN")
        );

        assertEquals("post-1", response.id());
        assertEquals("HIDDEN", response.moderationStatus().name());
        assertEquals(true, post.isHidden());
        assertEquals(false, post.isDeleted());
    }

    @Test
    void updatePostModeration_DELETED는_복구할수없다() {
        Post post = post("post-1", "member-1");
        post.markDeleted();
        when(postRepository.findByIdForAdminUpdate("post-1")).thenReturn(Optional.of(post));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> boardAdminService.updatePostModeration("post-1", new UpdateBoardModerationRequest("VISIBLE"))
        );

        assertEquals(ErrorCode.INVALID_POST_MODERATION_STATUS_TRANSITION, exception.getErrorCode());
    }

    @Test
    void updateCommentModeration_VISIBLE에서_HIDDEN으로_변경한다() {
        Post originalPost = post("post-1", "author-1");
        ReflectionTestUtils.setField(originalPost, "commentCount", 3);
        Comment comment = comment("comment-1", originalPost, null, "member-1");
        Post lockedPost = post("post-1", "author-1");
        ReflectionTestUtils.setField(lockedPost, "commentCount", 3);
        when(commentRepository.findByIdForAdminUpdate("comment-1")).thenReturn(Optional.of(comment));
        when(postRepository.findByIdForAdminUpdate("post-1")).thenReturn(Optional.of(lockedPost));

        BoardModerationResponse response = boardAdminService.updateCommentModeration(
                "comment-1",
                new UpdateBoardModerationRequest("HIDDEN")
        );

        assertEquals("comment-1", response.id());
        assertEquals("HIDDEN", response.moderationStatus().name());
        assertEquals(true, comment.isHidden());
        assertEquals(2, lockedPost.getCommentCount());
        assertEquals(3, originalPost.getCommentCount());
        verify(postRepository).findByIdForAdminUpdate("post-1");
    }

    @Test
    void updateCommentModeration_DELETED는_복구할수없다() {
        Post post = post("post-1", "author-1");
        Comment comment = comment("comment-1", post, null, "member-1");
        comment.softDelete();
        when(commentRepository.findByIdForAdminUpdate("comment-1")).thenReturn(Optional.of(comment));
        when(postRepository.findByIdForAdminUpdate("post-1")).thenReturn(Optional.of(post));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> boardAdminService.updateCommentModeration("comment-1", new UpdateBoardModerationRequest("VISIBLE"))
        );

        assertEquals(ErrorCode.INVALID_COMMENT_MODERATION_STATUS_TRANSITION, exception.getErrorCode());
    }

    private Post post(String id, String authorId) {
        Post post = Post.create("제목", "본문", authorId, "작성자", null, false, PostCategory.GENERAL);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private Comment comment(String id, Post post, Comment parent, String authorId) {
        Comment comment = Comment.create(post, "댓글", authorId, "댓글작성자", null, false, null, null, parent);
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
