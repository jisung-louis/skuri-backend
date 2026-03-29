package com.skuri.skuri_backend.domain.academic.repository;

import com.skuri.skuri_backend.domain.academic.entity.Course;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class CourseRepositoryDataJpaTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void search_필터조합으로_원하는강의만조회한다() {
        Course matched = course("01255", "민법총칙", "문상혁", "법학과", "2026-1", 2, 1, 3, 4);
        Course otherSemester = course("01255", "민법총칙", "문상혁", "법학과", "2025-2", 2, 1, 3, 4);
        Course otherProfessor = course("03210", "형법총론", "홍길동", "법학과", "2026-1", 2, 1, 3, 4);

        entityManager.persist(matched);
        entityManager.persist(otherSemester);
        entityManager.persist(otherProfessor);
        entityManager.flush();
        entityManager.clear();

        var result = courseRepository.search(
                "2026-1",
                "법학과",
                "문상혁",
                "민법",
                1,
                2,
                PageRequest.of(0, 20)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("민법총칙", result.getContent().get(0).getName());
    }

    private Course course(
            String code,
            String name,
            String professor,
            String department,
            String semester,
            int grade,
            int dayOfWeek,
            int startPeriod,
        int endPeriod
    ) {
        Course course = Course.create(grade, "전공선택", code, "001", name, 3, professor, "영401", null, false, semester, department);
        course.appendSchedule(dayOfWeek, startPeriod, endPeriod);
        return course;
    }
}
