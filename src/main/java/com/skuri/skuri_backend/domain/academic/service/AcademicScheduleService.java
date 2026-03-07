package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.request.CreateAcademicScheduleRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.UpdateAcademicScheduleRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.AcademicScheduleResponse;
import com.skuri.skuri_backend.domain.academic.entity.AcademicSchedule;
import com.skuri.skuri_backend.domain.academic.entity.AcademicScheduleType;
import com.skuri.skuri_backend.domain.academic.exception.AcademicScheduleNotFoundException;
import com.skuri.skuri_backend.domain.academic.repository.AcademicScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicScheduleService {

    private final AcademicScheduleRepository academicScheduleRepository;

    @Transactional(readOnly = true)
    public List<AcademicScheduleResponse> getSchedules(LocalDate startDate, LocalDate endDate, Boolean primary) {
        validateDateRange(startDate, endDate);
        return academicScheduleRepository.search(startDate, endDate, primary).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AcademicScheduleResponse createSchedule(CreateAcademicScheduleRequest request) {
        validateScheduleRequest(
                request.title(),
                request.startDate(),
                request.endDate(),
                request.type(),
                request.isPrimary()
        );
        AcademicSchedule academicSchedule = academicScheduleRepository.save(AcademicSchedule.create(
                request.title().trim(),
                request.startDate(),
                request.endDate(),
                request.type(),
                request.isPrimary(),
                trimToNull(request.description())
        ));
        return toResponse(academicSchedule);
    }

    @Transactional
    public AcademicScheduleResponse updateSchedule(String scheduleId, UpdateAcademicScheduleRequest request) {
        validateScheduleRequest(
                request.title(),
                request.startDate(),
                request.endDate(),
                request.type(),
                request.isPrimary()
        );
        AcademicSchedule academicSchedule = academicScheduleRepository.findById(scheduleId)
                .orElseThrow(AcademicScheduleNotFoundException::new);
        academicSchedule.update(
                request.title().trim(),
                request.startDate(),
                request.endDate(),
                request.type(),
                request.isPrimary(),
                trimToNull(request.description())
        );
        return toResponse(academicSchedule);
    }

    @Transactional
    public void deleteSchedule(String scheduleId) {
        AcademicSchedule academicSchedule = academicScheduleRepository.findById(scheduleId)
                .orElseThrow(AcademicScheduleNotFoundException::new);
        academicScheduleRepository.delete(academicSchedule);
    }

    private AcademicScheduleResponse toResponse(AcademicSchedule academicSchedule) {
        return new AcademicScheduleResponse(
                academicSchedule.getId(),
                academicSchedule.getTitle(),
                academicSchedule.getStartDate(),
                academicSchedule.getEndDate(),
                academicSchedule.getType(),
                academicSchedule.isPrimary(),
                academicSchedule.getDescription()
        );
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "startDate는 endDate보다 늦을 수 없습니다.");
        }
    }

    private void validateScheduleRequest(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            AcademicScheduleType type,
            Boolean primary
    ) {
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "title은 필수입니다.");
        }
        if (startDate == null || endDate == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "startDate와 endDate는 필수입니다.");
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "startDate는 endDate보다 늦을 수 없습니다.");
        }
        if (type == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "type은 필수입니다.");
        }
        if (type == AcademicScheduleType.SINGLE && !startDate.equals(endDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SINGLE 일정은 startDate와 endDate가 같아야 합니다.");
        }
        if (primary == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "isPrimary는 필수입니다.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
