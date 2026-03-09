package com.skuri.skuri_backend.domain.academic.repository;

import com.skuri.skuri_backend.domain.academic.entity.UserTimetable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface UserTimetableRepository extends JpaRepository<UserTimetable, String> {

    @Query("""
            select distinct t
            from UserTimetable t
            left join fetch t.courseMappings tc
            left join fetch tc.course c
            where t.userId = :userId
              and t.semester = :semester
            """)
    Optional<UserTimetable> findDetailByUserIdAndSemester(
            @Param("userId") String userId,
            @Param("semester") String semester
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select distinct t
            from UserTimetable t
            left join fetch t.courseMappings tc
            left join fetch tc.course c
            where t.userId = :userId
              and t.semester = :semester
            """)
    Optional<UserTimetable> findDetailByUserIdAndSemesterForUpdate(
            @Param("userId") String userId,
            @Param("semester") String semester
    );

    List<UserTimetable> findAllByUserId(String userId);
}
