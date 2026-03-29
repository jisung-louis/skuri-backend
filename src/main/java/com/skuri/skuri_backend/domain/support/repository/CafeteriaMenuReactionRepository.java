package com.skuri.skuri_backend.domain.support.repository;

import com.skuri.skuri_backend.domain.support.entity.CafeteriaMenuReaction;
import com.skuri.skuri_backend.domain.support.entity.CafeteriaMenuReactionId;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CafeteriaMenuReactionRepository extends JpaRepository<CafeteriaMenuReaction, CafeteriaMenuReactionId> {

    Optional<CafeteriaMenuReaction> findById_MemberIdAndId_MenuId(String memberId, String menuId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from CafeteriaMenuReaction r
            where r.weekId = :weekId
            """)
    void deleteByWeekId(@Param("weekId") String weekId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from CafeteriaMenuReaction r
            where r.weekId = :weekId
              and r.id.menuId not in :activeMenuIds
            """)
    void deleteObsoleteReactions(
            @Param("weekId") String weekId,
            @Param("activeMenuIds") Collection<String> activeMenuIds
    );

    @Query("""
            select r.id.menuId as menuId,
                   sum(case when r.reaction = com.skuri.skuri_backend.domain.support.model.CafeteriaMenuReactionType.LIKE then 1 else 0 end) as likeCount,
                   sum(case when r.reaction = com.skuri.skuri_backend.domain.support.model.CafeteriaMenuReactionType.DISLIKE then 1 else 0 end) as dislikeCount
            from CafeteriaMenuReaction r
            where r.weekId = :weekId
              and r.id.menuId in :menuIds
            group by r.id.menuId
            """)
    List<CafeteriaMenuReactionCountProjection> summarizeCounts(
            @Param("weekId") String weekId,
            @Param("menuIds") Collection<String> menuIds
    );

    @Query("""
            select r.id.menuId as menuId,
                   r.reaction as reaction
            from CafeteriaMenuReaction r
            where r.weekId = :weekId
              and r.id.memberId = :memberId
              and r.id.menuId in :menuIds
            """)
    List<CafeteriaMenuReactionSelectionProjection> findSelections(
            @Param("memberId") String memberId,
            @Param("weekId") String weekId,
            @Param("menuIds") Collection<String> menuIds
    );

    interface CafeteriaMenuReactionCountProjection {
        String getMenuId();

        long getLikeCount();

        long getDislikeCount();
    }

    interface CafeteriaMenuReactionSelectionProjection {
        String getMenuId();

        CafeteriaMenuReactionType getReaction();
    }
}
