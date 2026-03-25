package com.skuri.skuri_backend.domain.board.repository;

import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

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
