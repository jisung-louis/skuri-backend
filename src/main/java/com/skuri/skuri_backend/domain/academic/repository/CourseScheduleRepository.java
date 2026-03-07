package com.skuri.skuri_backend.domain.academic.repository;

import com.skuri.skuri_backend.domain.academic.entity.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from CourseSchedule cs
            where cs.course.id in :courseIds
            """)
    int deleteByCourseIds(@Param("courseIds") Collection<String> courseIds);
}
