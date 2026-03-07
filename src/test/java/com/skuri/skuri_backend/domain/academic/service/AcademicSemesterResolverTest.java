package com.skuri.skuri_backend.domain.academic.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcademicSemesterResolverTest {

    @Test
    void from_1월은_직전연도_2학기로해석한다() {
        assertEquals("2026-2", AcademicSemesterResolver.from(LocalDate.of(2027, 1, 15)));
    }

    @Test
    void from_2월부터_7월은_당해연도_1학기로해석한다() {
        assertEquals("2026-1", AcademicSemesterResolver.from(LocalDate.of(2026, 2, 1)));
        assertEquals("2026-1", AcademicSemesterResolver.from(LocalDate.of(2026, 7, 31)));
    }

    @Test
    void from_8월부터_12월은_당해연도_2학기로해석한다() {
        assertEquals("2026-2", AcademicSemesterResolver.from(LocalDate.of(2026, 8, 1)));
        assertEquals("2026-2", AcademicSemesterResolver.from(LocalDate.of(2026, 12, 31)));
    }
}
