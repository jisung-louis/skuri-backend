package com.skuri.skuri_backend.domain.academic.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_timetable_courses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTimetableCourse {

    @EmbeddedId
    private UserTimetableCourseId id;

    @MapsId("timetableId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false)
    private UserTimetable timetable;

    @MapsId("courseId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    private UserTimetableCourse(UserTimetable timetable, Course course) {
        this.id = UserTimetableCourseId.of(null, course.getId());
        this.timetable = timetable;
        this.course = course;
    }

    public static UserTimetableCourse create(UserTimetable timetable, Course course) {
        return new UserTimetableCourse(timetable, course);
    }

    public String getCourseId() {
        return id.getCourseId();
    }
}
