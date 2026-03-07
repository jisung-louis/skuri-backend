package com.skuri.skuri_backend.domain.academic.repository;

import com.skuri.skuri_backend.domain.academic.entity.UserTimetableCourse;
import com.skuri.skuri_backend.domain.academic.entity.UserTimetableCourseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface UserTimetableCourseRepository extends JpaRepository<UserTimetableCourse, UserTimetableCourseId> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from UserTimetableCourse utc
            where utc.course.id in :courseIds
            """)
    int deleteByCourseIds(@Param("courseIds") Collection<String> courseIds);
}
