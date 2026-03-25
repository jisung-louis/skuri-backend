package com.skuri.skuri_backend.domain.campus.repository;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.skuri.skuri_backend.domain.campus.entity.CampusBanner;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionTarget;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionType;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerPaletteKey;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class CampusBannerRepositoryDataJpaTest {

    @Autowired
    private CampusBannerRepository campusBannerRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findPublicVisible_노출조건과정렬규칙을적용한다() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 25, 12, 0);

        CampusBanner first = campusBannerRepository.save(campusBanner("택시 파티", 1, true, null, null));
        CampusBanner olderTie = campusBannerRepository.save(campusBanner("공지사항", 2, true, null, null));
        CampusBanner newerTie = campusBannerRepository.save(campusBanner("시간표", 2, true, null, null));
        campusBannerRepository.save(campusBanner("비활성", 3, false, null, null));
        campusBannerRepository.save(campusBanner("미래", 4, true, now.plusDays(1), null));
        campusBannerRepository.save(campusBanner("종료", 5, true, null, now.minusSeconds(1)));
        campusBannerRepository.flush();

        updateCreatedAt(first.getId(), now.minusDays(3));
        updateCreatedAt(olderTie.getId(), now.minusDays(2));
        updateCreatedAt(newerTie.getId(), now.minusDays(1));
        entityManager.clear();

        List<CampusBanner> result = campusBannerRepository.findPublicVisible(now);

        assertEquals(List.of(first.getId(), newerTie.getId(), olderTie.getId()), result.stream().map(CampusBanner::getId).toList());
    }

    @Test
    void findAllByOrderByDisplayOrderAscCreatedAtDesc_관리자정렬은비활성배너도포함한다() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 25, 12, 0);

        CampusBanner olderTie = campusBannerRepository.save(campusBanner("공지사항", 1, false, null, null));
        CampusBanner newerTie = campusBannerRepository.save(campusBanner("시간표", 1, true, null, null));
        CampusBanner last = campusBannerRepository.save(campusBanner("학식", 3, true, null, now.plusDays(1)));
        campusBannerRepository.flush();

        updateCreatedAt(olderTie.getId(), now.minusDays(2));
        updateCreatedAt(newerTie.getId(), now.minusDays(1));
        updateCreatedAt(last.getId(), now.minusHours(1));
        entityManager.clear();

        List<CampusBanner> result = campusBannerRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc();

        assertEquals(List.of(newerTie.getId(), olderTie.getId(), last.getId()), result.stream().map(CampusBanner::getId).toList());
    }

    private CampusBanner campusBanner(
            String titleLabel,
            int displayOrder,
            boolean active,
            LocalDateTime displayStartAt,
            LocalDateTime displayEndAt
    ) {
        return CampusBanner.create(
                "배지",
                titleLabel,
                "설명",
                "바로가기",
                CampusBannerPaletteKey.GREEN,
                "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner.jpg",
                CampusBannerActionType.IN_APP,
                CampusBannerActionTarget.TAXI_MAIN,
                JsonNodeFactory.instance.objectNode().put("initialView", "all"),
                null,
                active,
                displayStartAt,
                displayEndAt,
                displayOrder
        );
    }

    private void updateCreatedAt(String id, LocalDateTime createdAt) {
        entityManager.createNativeQuery("""
                update campus_banners
                set created_at = :createdAt,
                    updated_at = :createdAt
                where id = :id
                """)
                .setParameter("createdAt", createdAt)
                .setParameter("id", id)
                .executeUpdate();
    }
}
