package com.skuri.skuri_backend.domain.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "thumb_url", length = 500)
    private String thumbUrl;

    private Integer width;

    private Integer height;

    private Integer size;

    @Column(length = 50)
    private String mime;

    @Column(name = "sort_order")
    private Integer sortOrder;

    private PostImage(
            Post post,
            String url,
            String thumbUrl,
            Integer width,
            Integer height,
            Integer size,
            String mime,
            Integer sortOrder
    ) {
        this.post = post;
        this.url = url;
        this.thumbUrl = thumbUrl;
        this.width = width;
        this.height = height;
        this.size = size;
        this.mime = mime;
        this.sortOrder = sortOrder;
    }

    public static PostImage create(
            Post post,
            String url,
            String thumbUrl,
            Integer width,
            Integer height,
            Integer size,
            String mime,
            Integer sortOrder
    ) {
        return new PostImage(post, url, thumbUrl, width, height, size, mime, sortOrder);
    }
}
