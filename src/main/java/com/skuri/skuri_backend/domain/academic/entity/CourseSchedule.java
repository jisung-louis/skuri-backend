package com.skuri.skuri_backend.domain.academic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course_schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "start_period", nullable = false)
    private Integer startPeriod;

    @Column(name = "end_period", nullable = false)
    private Integer endPeriod;

    private CourseSchedule(Course course, Integer dayOfWeek, Integer startPeriod, Integer endPeriod) {
        this.course = course;
        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
    }

    public static CourseSchedule create(Course course, Integer dayOfWeek, Integer startPeriod, Integer endPeriod) {
        return new CourseSchedule(course, dayOfWeek, startPeriod, endPeriod);
    }

    public boolean overlaps(CourseSchedule other) {
        if (!dayOfWeek.equals(other.dayOfWeek)) {
            return false;
        }
        return startPeriod <= other.endPeriod && other.startPeriod <= endPeriod;
    }
}
