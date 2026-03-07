package com.skuri.skuri_backend.domain.academic.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(
        name = "academic_schedules",
        indexes = {
                @Index(name = "idx_academic_schedules_date", columnList = "start_date,end_date"),
                @Index(name = "idx_academic_schedules_primary", columnList = "is_primary")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AcademicSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AcademicScheduleType type;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Column(length = 500)
    private String description;

    private AcademicSchedule(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            AcademicScheduleType type,
            boolean isPrimary,
            String description
    ) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.isPrimary = isPrimary;
        this.description = description;
    }

    public static AcademicSchedule create(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            AcademicScheduleType type,
            boolean isPrimary,
            String description
    ) {
        return new AcademicSchedule(title, startDate, endDate, type, isPrimary, description);
    }

    public void update(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            AcademicScheduleType type,
            boolean isPrimary,
            String description
    ) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.isPrimary = isPrimary;
        this.description = description;
    }
}
