package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.domain.academic.dto.request.AdminBulkAcademicScheduleItemRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.AdminBulkAcademicSchedulesRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.AdminBulkAcademicSchedulesResponse;
import com.skuri.skuri_backend.domain.academic.entity.AcademicSchedule;
import com.skuri.skuri_backend.domain.academic.entity.AcademicScheduleType;
import com.skuri.skuri_backend.domain.academic.repository.AcademicScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(AcademicScheduleService.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class AcademicScheduleServiceDataJpaTest {

    @Autowired
    private AcademicScheduleService academicScheduleService;

    @Autowired
    private AcademicScheduleRepository academicScheduleRepository;

    @Test
    void bulkSyncSchedules_create_update_delete를_함께처리한다() {
        AcademicSchedule toUpdate = saveSchedule(
                "입학식 / 개강",
                LocalDate.of(2026, 3, 3),
                LocalDate.of(2026, 3, 3),
                AcademicScheduleType.SINGLE,
                false,
                "구 설명"
        );
        AcademicSchedule toDelete = saveSchedule(
                "삭제될 일정",
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 10),
                AcademicScheduleType.SINGLE,
                true,
                null
        );

        AdminBulkAcademicSchedulesResponse response = academicScheduleService.bulkSyncSchedules(new AdminBulkAcademicSchedulesRequest(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                List.of(
                        new AdminBulkAcademicScheduleItemRequest(
                                "입학식 / 개강",
                                LocalDate.of(2026, 3, 3),
                                LocalDate.of(2026, 3, 3),
                                "SINGLE",
                                true,
                                "정상수업"
                        ),
                        new AdminBulkAcademicScheduleItemRequest(
                                "수강신청 변경기간",
                                LocalDate.of(2026, 3, 4),
                                LocalDate.of(2026, 3, 9),
                                "MULTI",
                                true,
                                null
                        )
                )
        ));

        assertEquals(1, response.created());
        assertEquals(1, response.updated());
        assertEquals(1, response.deleted());
        assertTrue(academicScheduleRepository.findById(toUpdate.getId()).orElseThrow().isPrimary());
        assertEquals("정상수업", academicScheduleRepository.findById(toUpdate.getId()).orElseThrow().getDescription());
        assertTrue(academicScheduleRepository.findById(toDelete.getId()).isEmpty());
        assertEquals(2, academicScheduleRepository.count());
    }

    @Test
    void bulkSyncSchedules_scope밖기존일정은_보존한다() {
        AcademicSchedule partiallyOutsideScope = saveSchedule(
                "종강 채플",
                LocalDate.of(2026, 2, 27),
                LocalDate.of(2026, 3, 2),
                AcademicScheduleType.MULTI,
                false,
                null
        );

        AdminBulkAcademicSchedulesResponse response = academicScheduleService.bulkSyncSchedules(new AdminBulkAcademicSchedulesRequest(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                List.of(
                        new AdminBulkAcademicScheduleItemRequest(
                                "입학식 / 개강",
                                LocalDate.of(2026, 3, 3),
                                LocalDate.of(2026, 3, 3),
                                "SINGLE",
                                true,
                                "정상수업"
                        )
                )
        ));

        assertEquals(1, response.created());
        assertEquals(0, response.updated());
        assertEquals(0, response.deleted());
        assertTrue(academicScheduleRepository.findById(partiallyOutsideScope.getId()).isPresent());
        assertEquals(2, academicScheduleRepository.count());
    }

    @Test
    void bulkSyncSchedules_동일자연키면_변경가능필드만_업데이트한다() {
        AcademicSchedule existing = saveSchedule(
                "수강신청 변경기간",
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 9),
                AcademicScheduleType.MULTI,
                false,
                null
        );

        AdminBulkAcademicSchedulesResponse response = academicScheduleService.bulkSyncSchedules(new AdminBulkAcademicSchedulesRequest(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                List.of(
                        new AdminBulkAcademicScheduleItemRequest(
                                "수강신청 변경기간",
                                LocalDate.of(2026, 3, 4),
                                LocalDate.of(2026, 3, 9),
                                "MULTI",
                                true,
                                "정정 기간"
                        )
                )
        ));

        AcademicSchedule updated = academicScheduleRepository.findById(existing.getId()).orElseThrow();
        assertEquals(0, response.created());
        assertEquals(1, response.updated());
        assertEquals(0, response.deleted());
        assertEquals(existing.getId(), updated.getId());
        assertTrue(updated.isPrimary());
        assertEquals("정정 기간", updated.getDescription());
    }

    @Test
    void bulkSyncSchedules_type소문자입력을_정규화한다() {
        AdminBulkAcademicSchedulesResponse response = academicScheduleService.bulkSyncSchedules(new AdminBulkAcademicSchedulesRequest(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                List.of(
                        new AdminBulkAcademicScheduleItemRequest(
                                "입학식 / 개강",
                                LocalDate.of(2026, 3, 3),
                                LocalDate.of(2026, 3, 3),
                                "single",
                                true,
                                "정상수업"
                        ),
                        new AdminBulkAcademicScheduleItemRequest(
                                "수강신청 변경기간",
                                LocalDate.of(2026, 3, 4),
                                LocalDate.of(2026, 3, 9),
                                "multi",
                                true,
                                null
                        )
                )
        ));

        List<AcademicSchedule> schedules = academicScheduleRepository.search(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null
        );
        assertEquals(2, response.created());
        assertEquals(0, response.updated());
        assertEquals(0, response.deleted());
        assertEquals(List.of(AcademicScheduleType.SINGLE, AcademicScheduleType.MULTI), schedules.stream()
                .map(AcademicSchedule::getType)
                .toList());
    }

    private AcademicSchedule saveSchedule(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            AcademicScheduleType type,
            boolean isPrimary,
            String description
    ) {
        return academicScheduleRepository.saveAndFlush(AcademicSchedule.create(
                title,
                startDate,
                endDate,
                type,
                isPrimary,
                description
        ));
    }
}
