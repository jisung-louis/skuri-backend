package com.skuri.skuri_backend.domain.board.repository;

import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class PostImageRepositoryDataJpaTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostImageRepository postImageRepository;

    @Test
    void findFirstThumbnailByPostIds_첫이미지의_thumbUrl을우선하고_없으면_url을반환한다() {
        Post thumbPreferred = post("썸네일 우선");
        thumbPreferred.appendImage("https://example.com/post-1-original.jpg", "https://example.com/post-1-thumb.jpg", 800, 600, 1024, "image/jpeg", 0);
        thumbPreferred.appendImage("https://example.com/post-1-second.jpg", "https://example.com/post-1-second-thumb.jpg", 800, 600, 1024, "image/jpeg", 1);

        Post fallbackToOriginal = post("원본 fallback");
        fallbackToOriginal.appendImage("https://example.com/post-2-original.jpg", null, 800, 600, 1024, "image/jpeg", 0);
        fallbackToOriginal.appendImage("https://example.com/post-2-second.jpg", "https://example.com/post-2-second-thumb.jpg", 800, 600, 1024, "image/jpeg", 1);

        Post noImage = post("이미지 없음");

        postRepository.saveAll(List.of(thumbPreferred, fallbackToOriginal, noImage));
        postRepository.flush();

        Map<String, String> thumbnailByPostId = postImageRepository.findFirstThumbnailByPostIds(
                        List.of(thumbPreferred.getId(), fallbackToOriginal.getId(), noImage.getId())
                ).stream()
                .collect(Collectors.toMap(PostThumbnailProjection::getPostId, PostThumbnailProjection::getThumbnailUrl, (left, right) -> left));

        assertEquals("https://example.com/post-1-thumb.jpg", thumbnailByPostId.get(thumbPreferred.getId()));
        assertEquals("https://example.com/post-2-original.jpg", thumbnailByPostId.get(fallbackToOriginal.getId()));
        assertFalse(thumbnailByPostId.containsKey(noImage.getId()));
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
}
