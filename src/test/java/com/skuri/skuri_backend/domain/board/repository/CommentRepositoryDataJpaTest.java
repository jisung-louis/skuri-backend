package com.skuri.skuri_backend.domain.board.repository;

import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class CommentRepositoryDataJpaTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void findCommentedPostIds_삭제되지않은내댓글과대댓글이있는게시글만반환한다() {
        Post directCommented = postRepository.save(post("직접 댓글"));
        Post repliedPost = postRepository.save(post("대댓글 작성"));
        Post deletedMine = postRepository.save(post("삭제된 내 댓글"));
        Post othersOnly = postRepository.save(post("다른 사람 댓글"));

        commentRepository.save(comment(directCommented, null, "member-1"));
        Comment parent = commentRepository.save(comment(repliedPost, null, "member-2"));
        commentRepository.save(comment(repliedPost, parent, "member-1"));
        Comment deletedComment = commentRepository.save(comment(deletedMine, null, "member-1"));
        deletedComment.softDelete();
        commentRepository.save(comment(othersOnly, null, "member-2"));
        commentRepository.flush();

        List<String> result = commentRepository.findCommentedPostIds(
                "member-1",
                List.of(directCommented.getId(), repliedPost.getId(), deletedMine.getId(), othersOnly.getId())
        );

        assertEquals(Set.of(directCommented.getId(), repliedPost.getId()), Set.copyOf(result));
    }

    @Test
    void findActiveByAuthorId_삭제된게시글의댓글을제외하고최신순으로반환한다() {
        Post activePost = postRepository.save(post("활성 게시글"));
        Post deletedPost = postRepository.save(post("삭제된 게시글"));

        Comment visibleOlder = commentRepository.save(comment(activePost, null, "member-1"));
        Comment hiddenOnDeletedPost = commentRepository.save(comment(deletedPost, null, "member-1"));
        Comment visibleNewer = commentRepository.save(comment(activePost, null, "member-1"));
        commentRepository.flush();

        ReflectionTestUtils.setField(visibleOlder, "createdAt", LocalDateTime.of(2026, 3, 29, 10, 0));
        ReflectionTestUtils.setField(hiddenOnDeletedPost, "createdAt", LocalDateTime.of(2026, 3, 29, 11, 0));
        ReflectionTestUtils.setField(visibleNewer, "createdAt", LocalDateTime.of(2026, 3, 29, 12, 0));

        deletedPost.markDeleted();
        postRepository.flush();
        commentRepository.flush();

        Page<Comment> result = commentRepository.findActiveByAuthorId(
                "member-1",
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertEquals(2, result.getTotalElements());
        assertEquals(List.of(visibleNewer.getId(), visibleOlder.getId()),
                result.getContent().stream().map(Comment::getId).toList());
        assertEquals(List.of(activePost.getId(), activePost.getId()),
                result.getContent().stream().map(comment -> comment.getPost().getId()).toList());
    }

    private Post post(String title) {
        return Post.create(
                title,
                "본문",
                "author-1",
                "작성자",
                "https://example.com/profile.jpg",
                false,
                PostCategory.GENERAL
        );
    }

    private Comment comment(Post post, Comment parent, String authorId) {
        return Comment.create(
                post,
                "댓글",
                authorId,
                "작성자",
                "https://example.com/profile.jpg",
                false,
                null,
                null,
                parent
        );
    }
}
