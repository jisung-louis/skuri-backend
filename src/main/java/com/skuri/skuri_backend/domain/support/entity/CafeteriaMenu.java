package com.skuri.skuri_backend.domain.support.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.domain.support.entity.converter.CafeteriaMenuEntriesJsonConverter;
import com.skuri.skuri_backend.domain.support.entity.converter.CafeteriaMenusJsonConverter;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuEntryMetadata;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Entity
@Table(name = "cafeteria_menus")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CafeteriaMenu extends BaseTimeEntity {

    @Id
    @Column(name = "week_id", length = 20)
    private String weekId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Convert(converter = CafeteriaMenusJsonConverter.class)
    @Column(nullable = false, columnDefinition = "json")
    private Map<String, Map<String, List<String>>> menus = new LinkedHashMap<>();

    @Convert(converter = CafeteriaMenuEntriesJsonConverter.class)
    @Column(name = "menu_entries", columnDefinition = "json")
    private Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> menuEntries = new LinkedHashMap<>();

    private CafeteriaMenu(
            String weekId,
            LocalDate weekStart,
            LocalDate weekEnd,
            Map<String, Map<String, List<String>>> menus,
            Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> menuEntries
    ) {
        this.weekId = weekId;
        this.weekStart = weekStart;
        this.weekEnd = weekEnd;
        this.menus = normalizeMenus(menus);
        this.menuEntries = normalizeMenuEntries(menuEntries);
    }

    public static CafeteriaMenu create(
            String weekId,
            LocalDate weekStart,
            LocalDate weekEnd,
            Map<String, Map<String, List<String>>> menus,
            Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> menuEntries
    ) {
        return new CafeteriaMenu(weekId, weekStart, weekEnd, menus, menuEntries);
    }

    public void update(
            LocalDate weekStart,
            LocalDate weekEnd,
            Map<String, Map<String, List<String>>> menus,
            Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> menuEntries
    ) {
        this.weekStart = weekStart;
        this.weekEnd = weekEnd;
        this.menus = normalizeMenus(menus);
        this.menuEntries = normalizeMenuEntries(menuEntries);
    }

    private static Map<String, Map<String, List<String>>> normalizeMenus(
            Map<String, Map<String, List<String>>> source
    ) {
        Map<String, Map<String, List<String>>> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        source.forEach((date, restaurants) -> {
            Map<String, List<String>> restaurantMenus = new LinkedHashMap<>();
            if (restaurants != null) {
                restaurants.forEach((restaurant, items) -> restaurantMenus.put(
                        restaurant,
                        items == null ? List.of() : List.copyOf(items)
                ));
            }
            result.put(date, restaurantMenus);
        });
        return result;
    }

    private static Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> normalizeMenuEntries(
            Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> source
    ) {
        Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        source.forEach((date, categories) -> {
            Map<String, List<CafeteriaMenuEntryMetadata>> normalizedCategories = new LinkedHashMap<>();
            if (categories != null) {
                categories.forEach((category, items) -> normalizedCategories.put(
                        category,
                        items == null ? List.of() : List.copyOf(items)
                ));
            }
            result.put(date, normalizedCategories);
        });
        return result;
    }
}
