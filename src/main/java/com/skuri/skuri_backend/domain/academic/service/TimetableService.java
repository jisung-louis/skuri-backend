package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.request.AddMyTimetableCourseRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.CourseScheduleResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableCourseResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSlotResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.UserTimetableResponse;
import com.skuri.skuri_backend.domain.academic.entity.Course;
import com.skuri.skuri_backend.domain.academic.entity.CourseSchedule;
import com.skuri.skuri_backend.domain.academic.entity.UserTimetable;
import com.skuri.skuri_backend.domain.academic.entity.UserTimetableCourse;
import com.skuri.skuri_backend.domain.academic.exception.CourseAlreadyInTimetableException;
import com.skuri.skuri_backend.domain.academic.exception.CourseNotFoundException;
import com.skuri.skuri_backend.domain.academic.exception.TimetableConflictException;
import com.skuri.skuri_backend.domain.academic.repository.CourseRepository;
import com.skuri.skuri_backend.domain.academic.repository.UserTimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final UserTimetableRepository userTimetableRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public UserTimetableResponse getMyTimetable(String userId, String semester) {
        String resolvedSemester = AcademicSemesterResolver.resolve(semester, true);
        return userTimetableRepository.findDetailByUserIdAndSemester(userId, resolvedSemester)
                .map(this::toResponse)
                .orElseGet(() -> emptyResponse(resolvedSemester));
    }

    @Transactional
    public UserTimetableResponse addCourse(String userId, AddMyTimetableCourseRequest request) {
        String semester = AcademicSemesterResolver.require(request.semester());
        Course course = courseRepository.findDetailById(request.courseId()).orElseThrow(CourseNotFoundException::new);
        if (!course.getSemester().equals(semester)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "courseId와 semester가 일치하지 않습니다.");
        }

        UserTimetable timetable = findOrCreateTimetableForUpdate(userId, semester);
        if (timetable.containsCourse(course.getId())) {
            throw new CourseAlreadyInTimetableException();
        }
        if (hasScheduleConflict(timetable, course)) {
            throw new TimetableConflictException();
        }

        timetable.addCourse(course);
        return toResponse(timetable);
    }

    @Transactional
    public UserTimetableResponse removeCourse(String userId, String courseId, String semester) {
        String resolvedSemester = AcademicSemesterResolver.require(semester);
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException();
        }

        return userTimetableRepository.findDetailByUserIdAndSemesterForUpdate(userId, resolvedSemester)
                .map(timetable -> {
                    timetable.removeCourse(courseId);
                    return toResponse(timetable);
                })
                .orElseGet(() -> emptyResponse(resolvedSemester));
    }

    private UserTimetable findOrCreateTimetableForUpdate(String userId, String semester) {
        return userTimetableRepository.findDetailByUserIdAndSemesterForUpdate(userId, semester)
                .orElseGet(() -> createTimetable(userId, semester));
    }

    private UserTimetable createTimetable(String userId, String semester) {
        try {
            userTimetableRepository.saveAndFlush(UserTimetable.create(userId, semester));
        } catch (DataIntegrityViolationException e) {
            // 동일 사용자/학기 시간표가 동시에 생성된 경우 기존 row를 재조회한다.
        }
        return userTimetableRepository.findDetailByUserIdAndSemesterForUpdate(userId, semester)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "시간표 생성 중 충돌이 발생했습니다."));
    }

    private boolean hasScheduleConflict(UserTimetable timetable, Course targetCourse) {
        return fetchCoursesWithSchedules(timetable).stream()
                .anyMatch(existingCourse -> existingCourse.conflictsWith(targetCourse));
    }

    private UserTimetableResponse toResponse(UserTimetable timetable) {
        Map<String, Course> detailedCourses = fetchCoursesWithSchedules(timetable).stream()
                .collect(java.util.stream.Collectors.toMap(Course::getId, java.util.function.Function.identity()));

        List<Course> courses = timetable.getCourseMappings().stream()
                .map(UserTimetableCourse::getCourse)
                .map(course -> detailedCourses.getOrDefault(course.getId(), course))
                .sorted(courseComparator())
                .toList();

        List<TimetableCourseResponse> courseResponses = courses.stream()
                .map(this::toTimetableCourseResponse)
                .toList();
        List<TimetableSlotResponse> slotResponses = courses.stream()
                .flatMap(course -> course.getSchedules().stream()
                        .map(schedule -> toTimetableSlotResponse(course, schedule)))
                .sorted(Comparator
                        .comparing(TimetableSlotResponse::dayOfWeek)
                        .thenComparing(TimetableSlotResponse::startPeriod)
                        .thenComparing(TimetableSlotResponse::endPeriod)
                        .thenComparing(TimetableSlotResponse::courseName))
                .toList();

        int totalCredits = courses.stream()
                .map(Course::getCredits)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();

        return new UserTimetableResponse(
                timetable.getId(),
                timetable.getSemester(),
                courseResponses.size(),
                totalCredits,
                courseResponses,
                slotResponses
        );
    }

    private UserTimetableResponse emptyResponse(String semester) {
        return new UserTimetableResponse(null, semester, 0, 0, List.of(), List.of());
    }

    private List<Course> fetchCoursesWithSchedules(UserTimetable timetable) {
        List<String> courseIds = timetable.getCourseMappings().stream()
                .map(UserTimetableCourse::getCourseId)
                .toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return courseRepository.findAllWithSchedulesByIdIn(courseIds);
    }

    private TimetableCourseResponse toTimetableCourseResponse(Course course) {
        return new TimetableCourseResponse(
                course.getId(),
                course.getCode(),
                course.getDivision(),
                course.getName(),
                course.getProfessor(),
                course.getLocation(),
                course.getCategory(),
                course.getCredits(),
                course.getSchedules().stream()
                        .map(this::toScheduleResponse)
                        .toList()
        );
    }

    private TimetableSlotResponse toTimetableSlotResponse(Course course, CourseSchedule schedule) {
        return new TimetableSlotResponse(
                course.getId(),
                course.getName(),
                course.getCode(),
                schedule.getDayOfWeek(),
                schedule.getStartPeriod(),
                schedule.getEndPeriod(),
                course.getProfessor(),
                course.getLocation()
        );
    }

    private CourseScheduleResponse toScheduleResponse(CourseSchedule schedule) {
        return new CourseScheduleResponse(schedule.getDayOfWeek(), schedule.getStartPeriod(), schedule.getEndPeriod());
    }

    private Comparator<Course> courseComparator() {
        return Comparator
                .comparingInt(this::firstDayOfWeek)
                .thenComparingInt(this::firstStartPeriod)
                .thenComparing(Course::getName);
    }

    private int firstDayOfWeek(Course course) {
        return course.getSchedules().stream()
                .map(CourseSchedule::getDayOfWeek)
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);
    }

    private int firstStartPeriod(Course course) {
        return course.getSchedules().stream()
                .map(CourseSchedule::getStartPeriod)
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);
    }
}
