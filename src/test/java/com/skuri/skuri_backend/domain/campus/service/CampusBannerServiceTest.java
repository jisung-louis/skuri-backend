package com.skuri.skuri_backend.domain.campus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.campus.dto.request.CreateCampusBannerRequest;
import com.skuri.skuri_backend.domain.campus.dto.request.ReorderCampusBannersRequest;
import com.skuri.skuri_backend.domain.campus.dto.request.UpdateCampusBannerRequest;
import com.skuri.skuri_backend.domain.campus.dto.response.CampusBannerOrderResponse;
import com.skuri.skuri_backend.domain.campus.entity.CampusBanner;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionTarget;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionType;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerPaletteKey;
import com.skuri.skuri_backend.domain.campus.repository.CampusBannerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusBannerServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperConfig.SHARED_OBJECT_MAPPER.copy();

    @Mock
    private CampusBannerRepository campusBannerRepository;

    @InjectMocks
    private CampusBannerService campusBannerService;

    @Test
    void createBanner_기존순서를정규화하고_맨뒤에추가한다() {
        CampusBanner first = banner("banner-1", CampusBannerActionType.IN_APP, CampusBannerActionTarget.TAXI_MAIN, null, 2);
        CampusBanner second = banner("banner-2", CampusBannerActionType.IN_APP, CampusBannerActionTarget.NOTICE_MAIN, null, 5);

        when(campusBannerRepository.findAllAdminOrderedForUpdate()).thenReturn(new ArrayList<>(List.of(first, second)));
        when(campusBannerRepository.save(any(CampusBanner.class))).thenAnswer(invocation -> {
            CampusBanner saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "banner-3");
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 3, 25, 10, 0));
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.of(2026, 3, 25, 10, 0));
            return saved;
        });

        var response = campusBannerService.createBanner(new CreateCampusBannerRequest(
                " 택시 파티 ",
                " 택시 동승 매칭 ",
                " 같은 방향 가는 학생과 택시비를 함께 나눠요 ",
                " 파티 찾기 ",
                CampusBannerPaletteKey.GREEN,
                " https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg ",
                CampusBannerActionType.IN_APP,
                CampusBannerActionTarget.TAXI_MAIN,
                null,
                null,
                true,
                LocalDateTime.of(2026, 3, 25, 0, 0),
                null
        ));

        assertEquals(1, first.getDisplayOrder());
        assertEquals(2, second.getDisplayOrder());
        assertEquals(3, response.displayOrder());
        assertEquals("택시 파티", response.badgeLabel());
    }

    @Test
    void updateBanner_명시적null로_actionUrl을비우고_inApp으로전환한다() throws Exception {
        CampusBanner existing = banner("banner-1", CampusBannerActionType.EXTERNAL_URL, null, "https://www.sungkyul.ac.kr", 1);
        when(campusBannerRepository.findByIdForUpdate("banner-1")).thenReturn(Optional.of(existing));

        UpdateCampusBannerRequest request = OBJECT_MAPPER.readValue(
                """
                        {
                          "actionType": "IN_APP",
                          "actionTarget": "NOTICE_MAIN",
                          "actionParams": null,
                          "actionUrl": null,
                          "buttonLabel": "공지 보기"
                        }
                        """,
                UpdateCampusBannerRequest.class
        );

        var response = campusBannerService.updateBanner("banner-1", request);

        assertEquals(CampusBannerActionType.IN_APP, response.actionType());
        assertEquals(CampusBannerActionTarget.NOTICE_MAIN, response.actionTarget());
        assertNull(response.actionUrl());
        assertEquals("공지 보기", response.buttonLabel());
    }

    @Test
    void deleteBanner_삭제후순서를연속값으로정규화한다() {
        CampusBanner first = banner("banner-1", CampusBannerActionType.IN_APP, CampusBannerActionTarget.TAXI_MAIN, null, 1);
        CampusBanner second = banner("banner-2", CampusBannerActionType.IN_APP, CampusBannerActionTarget.NOTICE_MAIN, null, 2);
        CampusBanner third = banner("banner-3", CampusBannerActionType.IN_APP, CampusBannerActionTarget.TIMETABLE_DETAIL, null, 4);
        when(campusBannerRepository.findAllAdminOrderedForUpdate()).thenReturn(new ArrayList<>(List.of(first, second, third)));

        campusBannerService.deleteBanner("banner-2");

        verify(campusBannerRepository).delete(second);
        assertEquals(1, first.getDisplayOrder());
        assertEquals(2, third.getDisplayOrder());
    }

    @Test
    void reorderBanners_정상요청시_연속순서로재배치한다() {
        CampusBanner first = banner("banner-1", CampusBannerActionType.IN_APP, CampusBannerActionTarget.TAXI_MAIN, null, 1);
        CampusBanner second = banner("banner-2", CampusBannerActionType.IN_APP, CampusBannerActionTarget.NOTICE_MAIN, null, 2);
        CampusBanner third = banner("banner-3", CampusBannerActionType.IN_APP, CampusBannerActionTarget.TIMETABLE_DETAIL, null, 3);
        when(campusBannerRepository.findAllAdminOrderedForUpdate()).thenReturn(new ArrayList<>(List.of(first, second, third)));

        List<CampusBannerOrderResponse> response = campusBannerService.reorderBanners(
                new ReorderCampusBannersRequest(List.of("banner-2", "banner-3", "banner-1"))
        );

        assertEquals(List.of("banner-2", "banner-3", "banner-1"), response.stream().map(CampusBannerOrderResponse::id).toList());
        assertEquals(1, second.getDisplayOrder());
        assertEquals(2, third.getDisplayOrder());
        assertEquals(3, first.getDisplayOrder());
    }

    @Test
    void reorderBanners_중복id면_VALIDATION_ERROR() {
        CampusBanner first = banner("banner-1", CampusBannerActionType.IN_APP, CampusBannerActionTarget.TAXI_MAIN, null, 1);
        CampusBanner second = banner("banner-2", CampusBannerActionType.IN_APP, CampusBannerActionTarget.NOTICE_MAIN, null, 2);
        when(campusBannerRepository.findAllAdminOrderedForUpdate()).thenReturn(new ArrayList<>(List.of(first, second)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> campusBannerService.reorderBanners(new ReorderCampusBannersRequest(List.of("banner-1", "banner-1")))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    private CampusBanner banner(
            String id,
            CampusBannerActionType actionType,
            CampusBannerActionTarget actionTarget,
            String actionUrl,
            int displayOrder
    ) {
        CampusBanner banner = CampusBanner.create(
                "택시 파티",
                "택시 동승 매칭",
                "같은 방향 가는 학생과 택시비를 함께 나눠요",
                "파티 찾기",
                CampusBannerPaletteKey.GREEN,
                "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg",
                actionType,
                actionTarget,
                actionType == CampusBannerActionType.IN_APP
                        ? JsonNodeFactory.instance.objectNode().put("initialView", "all")
                        : null,
                actionUrl,
                true,
                LocalDateTime.of(2026, 3, 25, 0, 0),
                null,
                displayOrder
        );
        ReflectionTestUtils.setField(banner, "id", id);
        ReflectionTestUtils.setField(banner, "createdAt", LocalDateTime.of(2026, 3, 25, 10, 0));
        ReflectionTestUtils.setField(banner, "updatedAt", LocalDateTime.of(2026, 3, 25, 10, 0));
        return banner;
    }
}
