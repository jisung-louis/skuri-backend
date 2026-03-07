package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.support.dto.request.CreateCafeteriaMenuRequest;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateCafeteriaMenuRequest;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuResponse;
import com.skuri.skuri_backend.domain.support.entity.CafeteriaMenu;
import com.skuri.skuri_backend.domain.support.exception.CafeteriaMenuNotFoundException;
import com.skuri.skuri_backend.domain.support.repository.CafeteriaMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CafeteriaMenuService {

    private final CafeteriaMenuRepository cafeteriaMenuRepository;

    @Transactional(readOnly = true)
    public CafeteriaMenuResponse getCurrentWeekMenu(LocalDate date) {
        return getMenuByWeekId(toWeekId(date));
    }

    @Transactional(readOnly = true)
    public CafeteriaMenuResponse getMenuByWeekId(String weekId) {
        CafeteriaMenu cafeteriaMenu = cafeteriaMenuRepository.findById(normalizeWeekId(weekId))
                .orElseThrow(CafeteriaMenuNotFoundException::new);
        return toResponse(cafeteriaMenu);
    }

    @Transactional
    public CafeteriaMenuResponse createMenu(CreateCafeteriaMenuRequest request) {
        String normalizedWeekId = normalizeWeekId(request.weekId());
        validateWeekRange(normalizedWeekId, request.weekStart(), request.weekEnd());
        if (cafeteriaMenuRepository.existsById(normalizedWeekId)) {
            throw new BusinessException(ErrorCode.CAFETERIA_MENU_ALREADY_EXISTS);
        }

        CafeteriaMenu cafeteriaMenu = cafeteriaMenuRepository.save(CafeteriaMenu.create(
                normalizedWeekId,
                request.weekStart(),
                request.weekEnd(),
                normalizeMenus(request.menus())
        ));
        return toResponse(cafeteriaMenu);
    }

    @Transactional
    public CafeteriaMenuResponse updateMenu(String weekId, UpdateCafeteriaMenuRequest request) {
        String normalizedWeekId = normalizeWeekId(weekId);
        validateWeekRange(normalizedWeekId, request.weekStart(), request.weekEnd());
        CafeteriaMenu cafeteriaMenu = cafeteriaMenuRepository.findById(normalizedWeekId)
                .orElseThrow(CafeteriaMenuNotFoundException::new);
        cafeteriaMenu.update(request.weekStart(), request.weekEnd(), normalizeMenus(request.menus()));
        return toResponse(cafeteriaMenu);
    }

    @Transactional
    public void deleteMenu(String weekId) {
        CafeteriaMenu cafeteriaMenu = cafeteriaMenuRepository.findById(normalizeWeekId(weekId))
                .orElseThrow(CafeteriaMenuNotFoundException::new);
        cafeteriaMenuRepository.delete(cafeteriaMenu);
    }

    private CafeteriaMenuResponse toResponse(CafeteriaMenu cafeteriaMenu) {
        return new CafeteriaMenuResponse(
                cafeteriaMenu.getWeekId(),
                cafeteriaMenu.getWeekStart(),
                cafeteriaMenu.getWeekEnd(),
                deepCopyMenus(cafeteriaMenu.getMenus())
        );
    }

    private void validateWeekRange(String weekId, LocalDate weekStart, LocalDate weekEnd) {
        if (weekEnd.isBefore(weekStart)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "weekEnd는 weekStart보다 빠를 수 없습니다.");
        }
        if (!weekId.equals(toWeekId(weekStart))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "weekId와 weekStart가 일치하지 않습니다.");
        }
    }

    private String normalizeWeekId(String weekId) {
        if (!StringUtils.hasText(weekId) || !weekId.matches("^\\d{4}-W\\d{2}$")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "weekId 형식은 yyyy-Www 이어야 합니다.");
        }
        return weekId.trim().toUpperCase(Locale.ROOT);
    }

    public static String toWeekId(LocalDate date) {
        WeekFields weekFields = WeekFields.ISO;
        return "%04d-W%02d".formatted(date.get(weekFields.weekBasedYear()), date.get(weekFields.weekOfWeekBasedYear()));
    }

    private Map<String, Map<String, List<String>>> normalizeMenus(Map<String, Map<String, List<String>>> source) {
        Map<String, Map<String, List<String>>> result = new LinkedHashMap<>();
        source.forEach((date, restaurants) -> {
            if (!StringUtils.hasText(date)) {
                return;
            }
            Map<String, List<String>> normalizedRestaurants = new LinkedHashMap<>();
            if (restaurants != null) {
                restaurants.forEach((restaurant, items) -> {
                    if (!StringUtils.hasText(restaurant)) {
                        return;
                    }
                    List<String> normalizedItems = items == null
                            ? List.of()
                            : items.stream()
                            .filter(StringUtils::hasText)
                            .map(String::trim)
                            .toList();
                    normalizedRestaurants.put(restaurant.trim(), normalizedItems);
                });
            }
            result.put(date.trim(), normalizedRestaurants);
        });
        return result;
    }

    private Map<String, Map<String, List<String>>> deepCopyMenus(Map<String, Map<String, List<String>>> source) {
        Map<String, Map<String, List<String>>> copied = new LinkedHashMap<>();
        source.forEach((date, restaurants) -> {
            Map<String, List<String>> copiedRestaurants = new LinkedHashMap<>();
            restaurants.forEach((restaurant, items) -> copiedRestaurants.put(restaurant, List.copyOf(items)));
            copied.put(date, copiedRestaurants);
        });
        return copied;
    }
}
