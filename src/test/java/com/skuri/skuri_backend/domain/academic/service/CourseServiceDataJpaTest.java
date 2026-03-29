package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.domain.academic.dto.response.AdminBulkCoursesResponse;
import com.skuri.skuri_backend.domain.academic.entity.Course;
import com.skuri.skuri_backend.domain.academic.entity.UserTimetable;
import com.skuri.skuri_backend.domain.academic.repository.CourseRepository;
import com.skuri.skuri_backend.domain.academic.repository.CourseScheduleRepository;
import com.skuri.skuri_backend.domain.academic.repository.UserTimetableCourseRepository;
import com.skuri.skuri_backend.domain.academic.repository.UserTimetableRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import(CourseService.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class CourseServiceDataJpaTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseScheduleRepository courseScheduleRepository;

    @Autowired
    private UserTimetableRepository userTimetableRepository;

    @Autowired
    private UserTimetableCourseRepository userTimetableCourseRepository;

    @Test
    void deleteSemesterCourses_강의시간과시간표매핑까지함께삭제한다() {
        Course course = Course.create(2, "전공선택", "01255", "001", "민법총칙", 3, "문상혁", "영401", null, false, "2026-1", "법학과");
        course.appendSchedule(1, 3, 4);
        course = courseRepository.saveAndFlush(course);

        UserTimetable timetable = UserTimetable.create("user-1", "2026-1");
        timetable.addCourse(course);
        userTimetableRepository.saveAndFlush(timetable);

        AdminBulkCoursesResponse response = courseService.deleteSemesterCourses("2026-1");

        assertEquals(1, response.deleted());
        assertEquals(0, courseRepository.count());
        assertEquals(0, courseScheduleRepository.count());
        assertEquals(0, userTimetableCourseRepository.count());
    }
}
