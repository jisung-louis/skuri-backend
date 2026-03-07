package com.skuri.skuri_backend.domain.academic.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "courses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_courses_semester_code_division",
                columnNames = {"semester", "code", "division"}
        ),
        indexes = {
                @Index(name = "idx_courses_semester", columnList = "semester"),
                @Index(name = "idx_courses_department", columnList = "department"),
                @Index(name = "idx_courses_professor", columnList = "professor"),
                @Index(name = "idx_courses_code", columnList = "code")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Integer grade;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 10)
    private String division;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer credits;

    @Column(length = 50)
    private String professor;

    @Column(length = 100)
    private String location;

    @Column(length = 500)
    private String note;

    @Column(nullable = false, length = 10)
    private String semester;

    @Column(nullable = false, length = 50)
    private String department;

    @OrderBy("dayOfWeek ASC, startPeriod ASC, endPeriod ASC")
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<CourseSchedule> schedules = new ArrayList<>();

    private Course(
            Integer grade,
            String category,
            String code,
            String division,
            String name,
            Integer credits,
            String professor,
            String location,
            String note,
            String semester,
            String department
    ) {
        this.grade = grade;
        this.category = category;
        this.code = code;
        this.division = division;
        this.name = name;
        this.credits = credits;
        this.professor = professor;
        this.location = location;
        this.note = note;
        this.semester = semester;
        this.department = department;
    }

    public static Course create(
            Integer grade,
            String category,
            String code,
            String division,
            String name,
            Integer credits,
            String professor,
            String location,
            String note,
            String semester,
            String department
    ) {
        return new Course(grade, category, code, division, name, credits, professor, location, note, semester, department);
    }

    public void update(
            Integer grade,
            String category,
            String code,
            String division,
            String name,
            Integer credits,
            String professor,
            String location,
            String note,
            String department
    ) {
        this.grade = grade;
        this.category = category;
        this.code = code;
        this.division = division;
        this.name = name;
        this.credits = credits;
        this.professor = professor;
        this.location = location;
        this.note = note;
        this.department = department;
    }

    public void clearSchedules() {
        this.schedules.clear();
    }

    public void appendSchedule(int dayOfWeek, int startPeriod, int endPeriod) {
        this.schedules.add(CourseSchedule.create(this, dayOfWeek, startPeriod, endPeriod));
    }

    public boolean conflictsWith(Course other) {
        for (CourseSchedule mine : schedules) {
            for (CourseSchedule theirs : other.getSchedules()) {
                if (mine.overlaps(theirs)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String semesterCourseKey() {
        return semester + ":" + code + ":" + division;
    }
}
