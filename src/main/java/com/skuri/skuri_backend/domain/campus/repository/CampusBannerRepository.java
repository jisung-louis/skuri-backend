package com.skuri.skuri_backend.domain.campus.repository;

import com.skuri.skuri_backend.domain.campus.entity.CampusBanner;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CampusBannerRepository extends JpaRepository<CampusBanner, String> {

    @Query("""
            select b
            from CampusBanner b
            where b.active = true
              and (b.displayStartAt is null or b.displayStartAt <= :now)
              and (b.displayEndAt is null or b.displayEndAt > :now)
            order by b.displayOrder asc, b.createdAt desc
            """)
    List<CampusBanner> findPublicVisible(@Param("now") LocalDateTime now);

    List<CampusBanner> findAllByOrderByDisplayOrderAscCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from CampusBanner b
            order by b.displayOrder asc, b.createdAt desc
            """)
    List<CampusBanner> findAllAdminOrderedForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from CampusBanner b
            where b.id = :bannerId
            """)
    Optional<CampusBanner> findByIdForUpdate(@Param("bannerId") String bannerId);
}
