package com.skuri.skuri_backend.domain.board.repository;

import com.skuri.skuri_backend.domain.board.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    @Query("""
            select pi.post.id as postId,
                   case
                       when pi.thumbUrl is null or trim(pi.thumbUrl) = '' then pi.url
                       else pi.thumbUrl
                   end as thumbnailUrl
            from PostImage pi
            where pi.post.id in :postIds
              and not exists (
                    select other.id
                    from PostImage other
                    where other.post = pi.post
                      and (
                            coalesce(other.sortOrder, 2147483647) < coalesce(pi.sortOrder, 2147483647)
                            or (
                                coalesce(other.sortOrder, 2147483647) = coalesce(pi.sortOrder, 2147483647)
                                and other.id < pi.id
                            )
                      )
              )
            """)
    List<PostThumbnailProjection> findFirstThumbnailByPostIds(@Param("postIds") Collection<String> postIds);
}
