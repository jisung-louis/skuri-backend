package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.dto.request.AdminBulkAcademicScheduleItemRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.AdminBulkAcademicSchedulesRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.CreateAcademicScheduleRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.UpdateAcademicScheduleRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.AcademicScheduleResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.AdminBulkAcademicSchedulesResponse;
import com.skuri.skuri_backend.domain.academic.entity.AcademicSchedule;
import com.skuri.skuri_backend.domain.academic.entity.AcademicScheduleType;
import com.skuri.skuri_backend.domain.academic.exception.AcademicScheduleNotFoundException;
import com.skuri.skuri_backend.domain.academic.repository.AcademicScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
        NormalizedAcademicScheduleInput normalized = normalizeScheduleInput(
                request.title(),
                request.startDate(),
                request.endDate(),
                request.type(),
                request.isPrimary(),
                request.description()
        );
        AcademicSchedule academicSchedule = academicScheduleRepository.save(AcademicSchedule.create(
                normalized.title(),
                normalized.startDate(),
                normalized.endDate(),
                normalized.type(),
                normalized.isPrimary(),
                normalized.description()
        ));
        return toResponse(academicSchedule);
    }

    @Transactional
    public AcademicScheduleResponse updateSchedule(String scheduleId, UpdateAcademicScheduleRequest request) {
        NormalizedAcademicScheduleInput normalized = normalizeScheduleInput(
                request.title(),
                request.startDate(),
                request.endDate(),
                request.type(),
                request.isPrimary(),
                request.description()
        );
        AcademicSchedule academicSchedule = academicScheduleRepository.findById(scheduleId)
                .orElseThrow(AcademicScheduleNotFoundException::new);
        academicSchedule.update(
                normalized.title(),
                normalized.startDate(),
                normalized.endDate(),
                normalized.type(),
                normalized.isPrimary(),
                normalized.description()
        );
        return toResponse(academicSchedule);
    }

    @Transactional
    public AdminBulkAcademicSchedulesResponse bulkSyncSchedules(AdminBulkAcademicSchedulesRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "request는 필수입니다.");
        }
        if (request.scopeStartDate() == null || request.scopeEndDate() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "scopeStartDate와 scopeEndDate는 필수입니다.");
        }
        if (request.scopeStartDate().isAfter(request.scopeEndDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "scopeStartDate는 scopeEndDate보다 늦을 수 없습니다.");
        }
        if (request.schedules() == null || request.schedules().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "schedules는 최소 1개 이상이어야 합니다.");
        }

        LocalDate scopeStartDate = request.scopeStartDate();
        LocalDate scopeEndDate = request.scopeEndDate();

        List<NormalizedAcademicScheduleInput> normalizedSchedules = new ArrayList<>(request.schedules().size());
        Set<AcademicScheduleNaturalKey> requestedNaturalKeys = new LinkedHashSet<>();

        for (AdminBulkAcademicScheduleItemRequest scheduleRequest : request.schedules()) {
            if (scheduleRequest == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "schedules 항목은 null일 수 없습니다.");
            }
            NormalizedAcademicScheduleInput normalized = normalizeBulkScheduleInput(
                    scheduleRequest,
                    scopeStartDate,
                    scopeEndDate
            );
            if (!requestedNaturalKeys.add(normalized.naturalKey())) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "같은 title/startDate/endDate/type 조합이 요청에 중복되었습니다: "
                                + normalized.naturalKey().summary()
                );
            }
            normalizedSchedules.add(normalized);
        }

        List<AcademicSchedule> existingSchedules = academicScheduleRepository.search(scopeStartDate, scopeEndDate, null).stream()
                .filter(schedule -> isWithinScope(schedule.getStartDate(), schedule.getEndDate(), scopeStartDate, scopeEndDate))
                .toList();
        Map<AcademicScheduleNaturalKey, Deque<AcademicSchedule>> existingBuckets = new LinkedHashMap<>();
        for (AcademicSchedule existingSchedule : existingSchedules) {
            existingBuckets.computeIfAbsent(naturalKey(existingSchedule), ignored -> new ArrayDeque<>())
                    .addLast(existingSchedule);
        }

        int created = 0;
        int updated = 0;
        Set<String> retainedExistingIds = new LinkedHashSet<>();

        for (NormalizedAcademicScheduleInput normalized : normalizedSchedules) {
            Deque<AcademicSchedule> bucket = existingBuckets.get(normalized.naturalKey());
            AcademicSchedule matchedSchedule = bucket == null ? null : bucket.pollFirst();

            if (matchedSchedule == null) {
                academicScheduleRepository.save(AcademicSchedule.create(
                        normalized.title(),
                        normalized.startDate(),
                        normalized.endDate(),
                        normalized.type(),
                        normalized.isPrimary(),
                        normalized.description()
                ));
                created++;
                continue;
            }

            retainedExistingIds.add(matchedSchedule.getId());
            if (requiresUpdate(matchedSchedule, normalized)) {
                matchedSchedule.update(
                        normalized.title(),
                        normalized.startDate(),
                        normalized.endDate(),
                        normalized.type(),
                        normalized.isPrimary(),
                        normalized.description()
                );
                updated++;
            }
        }

        List<AcademicSchedule> schedulesToDelete = existingSchedules.stream()
                .filter(schedule -> !retainedExistingIds.contains(schedule.getId()))
                .toList();
        if (!schedulesToDelete.isEmpty()) {
            academicScheduleRepository.deleteAll(schedulesToDelete);
        }

        return new AdminBulkAcademicSchedulesResponse(
                scopeStartDate,
                scopeEndDate,
                created,
                updated,
                schedulesToDelete.size()
        );
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
            Boolean primary,
            String description
    ) {
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "title은 필수입니다.");
        }
        if (title.trim().length() > 200) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "title은 200자 이하여야 합니다.");
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
        String normalizedDescription = trimToNull(description);
        if (normalizedDescription != null && normalizedDescription.length() > 500) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "description은 500자 이하여야 합니다.");
        }
    }

    private NormalizedAcademicScheduleInput normalizeScheduleInput(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            AcademicScheduleType type,
            Boolean primary,
            String description
    ) {
        validateScheduleRequest(title, startDate, endDate, type, primary, description);
        return new NormalizedAcademicScheduleInput(
                title.trim(),
                startDate,
                endDate,
                type,
                primary,
                trimToNull(description)
        );
    }

    private NormalizedAcademicScheduleInput normalizeBulkScheduleInput(
            AdminBulkAcademicScheduleItemRequest request,
            LocalDate scopeStartDate,
            LocalDate scopeEndDate
    ) {
        NormalizedAcademicScheduleInput normalized = normalizeScheduleInput(
                request.title(),
                request.startDate(),
                request.endDate(),
                normalizeBulkType(request.type()),
                request.isPrimary(),
                request.description()
        );
        if (!isWithinScope(normalized.startDate(), normalized.endDate(), scopeStartDate, scopeEndDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "모든 일정은 scopeStartDate와 scopeEndDate 범위 안에 있어야 합니다.");
        }
        return normalized;
    }

    private AcademicScheduleType normalizeBulkType(String rawType) {
        String normalized = trimToNull(rawType);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "type은 필수입니다.");
        }
        try {
            return AcademicScheduleType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "type은 SINGLE 또는 MULTI여야 합니다.");
        }
    }

    private boolean isWithinScope(
            LocalDate startDate,
            LocalDate endDate,
            LocalDate scopeStartDate,
            LocalDate scopeEndDate
    ) {
        return !startDate.isBefore(scopeStartDate) && !endDate.isAfter(scopeEndDate);
    }

    private boolean requiresUpdate(AcademicSchedule academicSchedule, NormalizedAcademicScheduleInput normalized) {
        return !Objects.equals(academicSchedule.getTitle(), normalized.title())
                || !Objects.equals(academicSchedule.getStartDate(), normalized.startDate())
                || !Objects.equals(academicSchedule.getEndDate(), normalized.endDate())
                || academicSchedule.getType() != normalized.type()
                || academicSchedule.isPrimary() != normalized.isPrimary()
                || !Objects.equals(academicSchedule.getDescription(), normalized.description());
    }

    private AcademicScheduleNaturalKey naturalKey(AcademicSchedule academicSchedule) {
        return new AcademicScheduleNaturalKey(
                academicSchedule.getTitle().trim(),
                academicSchedule.getStartDate(),
                academicSchedule.getEndDate(),
                academicSchedule.getType()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record NormalizedAcademicScheduleInput(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            AcademicScheduleType type,
            boolean isPrimary,
            String description
    ) {
        private AcademicScheduleNaturalKey naturalKey() {
            return new AcademicScheduleNaturalKey(title, startDate, endDate, type);
        }
    }

    private record AcademicScheduleNaturalKey(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            AcademicScheduleType type
    ) {
        private String summary() {
            return title + "|" + startDate + "|" + endDate + "|" + type;
        }
    }
}
