package com.skuri.skuri_backend.domain.academic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTimetableCourseId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "timetable_id", length = 36)
    private String timetableId;

    @Column(name = "course_id", length = 36)
    private String courseId;

    private UserTimetableCourseId(String timetableId, String courseId) {
        this.timetableId = timetableId;
        this.courseId = courseId;
    }

    public static UserTimetableCourseId of(String timetableId, String courseId) {
        return new UserTimetableCourseId(timetableId, courseId);
    }
}
