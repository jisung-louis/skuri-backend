package com.skuri.skuri_backend.domain.academic.repository;

import com.skuri.skuri_backend.domain.academic.entity.AcademicSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AcademicScheduleRepository extends JpaRepository<AcademicSchedule, String> {

    List<AcademicSchedule> findByStartDateOrderByCreatedAtAsc(LocalDate startDate);

    @Query("""
            select s
            from AcademicSchedule s
            where (:startDate is null or s.endDate >= :startDate)
              and (:endDate is null or s.startDate <= :endDate)
              and (:primary is null or s.isPrimary = :primary)
            order by s.startDate asc, s.endDate asc, s.isPrimary desc, s.createdAt asc
            """)
    List<AcademicSchedule> search(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("primary") Boolean primary
    );
}
