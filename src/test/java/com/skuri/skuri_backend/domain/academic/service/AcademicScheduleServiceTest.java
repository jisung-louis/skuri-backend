package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.request.CreateAcademicScheduleRequest;
import com.skuri.skuri_backend.domain.academic.entity.AcademicSchedule;
import com.skuri.skuri_backend.domain.academic.entity.AcademicScheduleType;
import com.skuri.skuri_backend.domain.academic.repository.AcademicScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicScheduleServiceTest {

    @Mock
    private AcademicScheduleRepository academicScheduleRepository;

    @InjectMocks
    private AcademicScheduleService academicScheduleService;

    @Test
    void createSchedule_정상생성() {
        when(academicScheduleRepository.save(any(AcademicSchedule.class))).thenAnswer(invocation -> {
            AcademicSchedule saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "schedule-1");
            return saved;
        });

        var response = academicScheduleService.createSchedule(new CreateAcademicScheduleRequest(
                "중간고사",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 21),
                AcademicScheduleType.MULTI,
                true,
                "2026학년도 1학기 중간고사"
        ));

        assertEquals("schedule-1", response.id());
        assertEquals(AcademicScheduleType.MULTI, response.type());
    }

    @Test
    void createSchedule_SINGLE인데기간이여러날이면_예외() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> academicScheduleService.createSchedule(new CreateAcademicScheduleRequest(
                        "개강",
                        LocalDate.of(2026, 3, 2),
                        LocalDate.of(2026, 3, 3),
                        AcademicScheduleType.SINGLE,
                        true,
                        null
                ))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }
}
