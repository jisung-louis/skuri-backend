package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.request.AddMyTimetableCourseRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.UserTimetableResponse;
import com.skuri.skuri_backend.domain.academic.entity.Course;
import com.skuri.skuri_backend.domain.academic.entity.UserTimetable;
import com.skuri.skuri_backend.domain.academic.repository.CourseRepository;
import com.skuri.skuri_backend.domain.academic.repository.UserTimetableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock
    private UserTimetableRepository userTimetableRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private TimetableService timetableService;

    @Test
    void addCourse_정상추가() {
        Course targetCourse = course("course-1", "01255", "민법총칙", "2026-1", 1, 3, 4);
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");

        when(courseRepository.findDetailById("course-1")).thenReturn(Optional.of(targetCourse));
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1")).thenReturn(Optional.of(timetable));
        when(courseRepository.findAllWithSchedulesByIdIn(List.of("course-1"))).thenReturn(List.of(targetCourse));

        UserTimetableResponse response = timetableService.addCourse(
                "user-1",
                new AddMyTimetableCourseRequest("course-1", "2026-1")
        );

        assertEquals(1, response.courseCount());
        assertEquals(3, response.totalCredits());
        assertEquals("course-1", response.courses().get(0).id());
        assertEquals("민법총칙", response.slots().get(0).courseName());
    }

    @Test
    void addCourse_같은강의중복이면_예외() {
        Course targetCourse = course("course-1", "01255", "민법총칙", "2026-1", 1, 3, 4);
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");
        timetable.addCourse(targetCourse);

        when(courseRepository.findDetailById("course-1")).thenReturn(Optional.of(targetCourse));
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1")).thenReturn(Optional.of(timetable));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> timetableService.addCourse("user-1", new AddMyTimetableCourseRequest("course-1", "2026-1"))
        );

        assertEquals(ErrorCode.COURSE_ALREADY_IN_TIMETABLE, exception.getErrorCode());
    }

    @Test
    void addCourse_시간이겹치면_예외() {
        Course existingCourse = course("course-existing", "01255", "민법총칙", "2026-1", 1, 3, 4);
        Course targetCourse = course("course-new", "03210", "형법총론", "2026-1", 1, 4, 5);
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");
        timetable.addCourse(existingCourse);

        when(courseRepository.findDetailById("course-new")).thenReturn(Optional.of(targetCourse));
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1")).thenReturn(Optional.of(timetable));
        when(courseRepository.findAllWithSchedulesByIdIn(List.of("course-existing"))).thenReturn(List.of(existingCourse));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> timetableService.addCourse("user-1", new AddMyTimetableCourseRequest("course-new", "2026-1"))
        );

        assertEquals(ErrorCode.TIMETABLE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void removeCourse_정상삭제() {
        Course existingCourse = course("course-1", "01255", "민법총칙", "2026-1", 1, 3, 4);
        UserTimetable timetable = timetable("timetable-1", "user-1", "2026-1");
        timetable.addCourse(existingCourse);

        when(courseRepository.existsById("course-1")).thenReturn(true);
        when(userTimetableRepository.findDetailByUserIdAndSemesterForUpdate("user-1", "2026-1")).thenReturn(Optional.of(timetable));

        UserTimetableResponse response = timetableService.removeCourse("user-1", "course-1", "2026-1");

        assertEquals(0, response.courseCount());
        assertEquals(0, response.totalCredits());
    }

    @Test
    void deleteAllByUserId_회원탈퇴시_시간표를전부삭제한다() {
        UserTimetable first = timetable("timetable-1", "user-1", "2026-1");
        UserTimetable second = timetable("timetable-2", "user-1", "2026-2");
        when(userTimetableRepository.findAllByUserId("user-1")).thenReturn(List.of(first, second));

        timetableService.deleteAllByUserId("user-1");

        verify(userTimetableRepository).deleteAll(List.of(first, second));
    }

    private Course course(
            String id,
            String code,
            String name,
            String semester,
            int dayOfWeek,
            int startPeriod,
            int endPeriod
    ) {
        Course course = Course.create(2, "전공선택", code, "001", name, 3, "문상혁", "영401", null, semester, "법학과");
        ReflectionTestUtils.setField(course, "id", id);
        course.appendSchedule(dayOfWeek, startPeriod, endPeriod);
        return course;
    }

    private UserTimetable timetable(String id, String userId, String semester) {
        UserTimetable timetable = UserTimetable.create(userId, semester);
        ReflectionTestUtils.setField(timetable, "id", id);
        return timetable;
    }
}
