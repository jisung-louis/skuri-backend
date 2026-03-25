package com.skuri.skuri_backend.domain.campus.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.campus.dto.request.CreateCampusBannerRequest;
import com.skuri.skuri_backend.domain.campus.dto.request.ReorderCampusBannersRequest;
import com.skuri.skuri_backend.domain.campus.dto.request.UpdateCampusBannerRequest;
import com.skuri.skuri_backend.domain.campus.dto.response.CampusBannerAdminResponse;
import com.skuri.skuri_backend.domain.campus.dto.response.CampusBannerOrderResponse;
import com.skuri.skuri_backend.domain.campus.dto.response.CampusBannerPublicResponse;
import com.skuri.skuri_backend.domain.campus.entity.CampusBanner;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionTarget;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionType;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerPaletteKey;
import com.skuri.skuri_backend.domain.campus.exception.CampusBannerNotFoundException;
import com.skuri.skuri_backend.domain.campus.repository.CampusBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampusBannerService {

    private final CampusBannerRepository campusBannerRepository;

    @Transactional(readOnly = true)
    public List<CampusBannerPublicResponse> getPublicBanners() {
        return campusBannerRepository.findPublicVisible(LocalDateTime.now()).stream()
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CampusBannerAdminResponse> getAdminBanners() {
        return campusBannerRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CampusBannerAdminResponse getAdminBanner(String bannerId) {
        CampusBanner campusBanner = campusBannerRepository.findById(bannerId)
                .orElseThrow(CampusBannerNotFoundException::new);
        return toAdminResponse(campusBanner);
    }

    @Transactional
    public CampusBannerAdminResponse createBanner(CreateCampusBannerRequest request) {
        List<CampusBanner> orderedBanners = campusBannerRepository.findAllAdminOrderedForUpdate();
        normalizeDisplayOrders(orderedBanners);

        String badgeLabel = normalizeRequiredText(request.badgeLabel(), "badgeLabel");
        String titleLabel = normalizeRequiredText(request.titleLabel(), "titleLabel");
        String descriptionLabel = normalizeRequiredText(request.descriptionLabel(), "descriptionLabel");
        String buttonLabel = normalizeRequiredText(request.buttonLabel(), "buttonLabel");
        CampusBannerPaletteKey paletteKey = requireValue(request.paletteKey(), "paletteKey");
        String imageUrl = normalizeRequiredText(request.imageUrl(), "imageUrl");
        ActionValues actionValues = normalizeAction(
                request.actionType(),
                request.actionTarget(),
                request.actionParams(),
                request.actionUrl()
        );
        boolean active = requireBoolean(request.isActive(), "isActive");
        validateDisplayWindow(request.displayStartAt(), request.displayEndAt());

        CampusBanner campusBanner = campusBannerRepository.save(CampusBanner.create(
                badgeLabel,
                titleLabel,
                descriptionLabel,
                buttonLabel,
                paletteKey,
                imageUrl,
                actionValues.actionType(),
                actionValues.actionTarget(),
                actionValues.actionParams(),
                actionValues.actionUrl(),
                active,
                request.displayStartAt(),
                request.displayEndAt(),
                orderedBanners.size() + 1
        ));

        return toAdminResponse(campusBanner);
    }

    @Transactional
    public CampusBannerAdminResponse updateBanner(String bannerId, UpdateCampusBannerRequest request) {
        CampusBanner campusBanner = campusBannerRepository.findByIdForUpdate(bannerId)
                .orElseThrow(CampusBannerNotFoundException::new);

        String badgeLabel = request.isBadgeLabelPresent()
                ? normalizeRequiredText(request.getBadgeLabel(), "badgeLabel")
                : campusBanner.getBadgeLabel();
        String titleLabel = request.isTitleLabelPresent()
                ? normalizeRequiredText(request.getTitleLabel(), "titleLabel")
                : campusBanner.getTitleLabel();
        String descriptionLabel = request.isDescriptionLabelPresent()
                ? normalizeRequiredText(request.getDescriptionLabel(), "descriptionLabel")
                : campusBanner.getDescriptionLabel();
        String buttonLabel = request.isButtonLabelPresent()
                ? normalizeRequiredText(request.getButtonLabel(), "buttonLabel")
                : campusBanner.getButtonLabel();
        CampusBannerPaletteKey paletteKey = request.isPaletteKeyPresent()
                ? requireValue(request.getPaletteKey(), "paletteKey")
                : campusBanner.getPaletteKey();
        String imageUrl = request.isImageUrlPresent()
                ? normalizeRequiredText(request.getImageUrl(), "imageUrl")
                : campusBanner.getImageUrl();
        CampusBannerActionType actionType = request.isActionTypePresent()
                ? requireValue(request.getActionType(), "actionType")
                : campusBanner.getActionType();
        CampusBannerActionTarget actionTarget = request.isActionTargetPresent()
                ? request.getActionTarget()
                : campusBanner.getActionTarget();
        JsonNode actionParams = request.isActionParamsPresent()
                ? request.getActionParams()
                : campusBanner.getActionParams();
        String actionUrl = request.isActionUrlPresent()
                ? request.getActionUrl()
                : campusBanner.getActionUrl();
        ActionValues actionValues = normalizeAction(actionType, actionTarget, actionParams, actionUrl);
        boolean active = request.isActivePresent()
                ? requireBoolean(request.getIsActive(), "isActive")
                : campusBanner.isActive();
        LocalDateTime displayStartAt = request.isDisplayStartAtPresent()
                ? request.getDisplayStartAt()
                : campusBanner.getDisplayStartAt();
        LocalDateTime displayEndAt = request.isDisplayEndAtPresent()
                ? request.getDisplayEndAt()
                : campusBanner.getDisplayEndAt();

        validateDisplayWindow(displayStartAt, displayEndAt);

        campusBanner.update(
                badgeLabel,
                titleLabel,
                descriptionLabel,
                buttonLabel,
                paletteKey,
                imageUrl,
                actionValues.actionType(),
                actionValues.actionTarget(),
                actionValues.actionParams(),
                actionValues.actionUrl(),
                active,
                displayStartAt,
                displayEndAt
        );

        return toAdminResponse(campusBanner);
    }

    @Transactional
    public void deleteBanner(String bannerId) {
        List<CampusBanner> orderedBanners = campusBannerRepository.findAllAdminOrderedForUpdate();
        normalizeDisplayOrders(orderedBanners);

        CampusBanner target = orderedBanners.stream()
                .filter(banner -> banner.getId().equals(bannerId))
                .findFirst()
                .orElseThrow(CampusBannerNotFoundException::new);

        campusBannerRepository.delete(target);
        orderedBanners.remove(target);
        normalizeDisplayOrders(orderedBanners);
    }

    @Transactional
    public List<CampusBannerOrderResponse> reorderBanners(ReorderCampusBannersRequest request) {
        List<CampusBanner> orderedBanners = campusBannerRepository.findAllAdminOrderedForUpdate();
        normalizeDisplayOrders(orderedBanners);

        List<String> normalizedBannerIds = normalizeBannerIds(request.bannerIds());
        validateReorderIds(normalizedBannerIds, orderedBanners);

        Map<String, CampusBanner> campusBannerById = orderedBanners.stream()
                .collect(Collectors.toMap(CampusBanner::getId, Function.identity()));

        List<CampusBannerOrderResponse> responses = new ArrayList<>();
        int displayOrder = 1;
        for (String bannerId : normalizedBannerIds) {
            CampusBanner campusBanner = campusBannerById.get(bannerId);
            campusBanner.changeDisplayOrder(displayOrder);
            responses.add(new CampusBannerOrderResponse(campusBanner.getId(), displayOrder));
            displayOrder++;
        }
        return responses;
    }

    private CampusBannerPublicResponse toPublicResponse(CampusBanner campusBanner) {
        return new CampusBannerPublicResponse(
                campusBanner.getId(),
                campusBanner.getBadgeLabel(),
                campusBanner.getTitleLabel(),
                campusBanner.getDescriptionLabel(),
                campusBanner.getButtonLabel(),
                campusBanner.getPaletteKey(),
                campusBanner.getImageUrl(),
                campusBanner.getActionType(),
                campusBanner.getActionTarget(),
                copyActionParams(campusBanner.getActionParams()),
                campusBanner.getActionUrl()
        );
    }

    private CampusBannerAdminResponse toAdminResponse(CampusBanner campusBanner) {
        return new CampusBannerAdminResponse(
                campusBanner.getId(),
                campusBanner.getBadgeLabel(),
                campusBanner.getTitleLabel(),
                campusBanner.getDescriptionLabel(),
                campusBanner.getButtonLabel(),
                campusBanner.getPaletteKey(),
                campusBanner.getImageUrl(),
                campusBanner.getActionType(),
                campusBanner.getActionTarget(),
                copyActionParams(campusBanner.getActionParams()),
                campusBanner.getActionUrl(),
                campusBanner.isActive(),
                campusBanner.getDisplayStartAt(),
                campusBanner.getDisplayEndAt(),
                campusBanner.getDisplayOrder(),
                campusBanner.getCreatedAt(),
                campusBanner.getUpdatedAt()
        );
    }

    private void validateDisplayWindow(LocalDateTime displayStartAt, LocalDateTime displayEndAt) {
        if (displayStartAt != null && displayEndAt != null && displayEndAt.isBefore(displayStartAt)) {
            throw validationError("displayEndAt은 displayStartAt보다 빠를 수 없습니다.");
        }
    }

    private ActionValues normalizeAction(
            CampusBannerActionType actionType,
            CampusBannerActionTarget actionTarget,
            JsonNode actionParams,
            String actionUrl
    ) {
        CampusBannerActionType normalizedActionType = requireValue(actionType, "actionType");
        JsonNode normalizedActionParams = normalizeActionParams(actionParams);
        String normalizedActionUrl = trimToNull(actionUrl);

        if (normalizedActionType == CampusBannerActionType.IN_APP) {
            if (actionTarget == null) {
                throw validationError("IN_APP actionType에는 actionTarget이 필요합니다.");
            }
            if (normalizedActionUrl != null) {
                throw validationError("IN_APP actionType에는 actionUrl을 설정할 수 없습니다.");
            }
            return new ActionValues(normalizedActionType, actionTarget, normalizedActionParams, null);
        }

        if (normalizedActionUrl == null) {
            throw validationError("EXTERNAL_URL actionType에는 actionUrl이 필요합니다.");
        }
        if (actionTarget != null) {
            throw validationError("EXTERNAL_URL actionType에는 actionTarget을 설정할 수 없습니다.");
        }
        if (normalizedActionParams != null) {
            throw validationError("EXTERNAL_URL actionType에는 actionParams를 설정할 수 없습니다.");
        }

        return new ActionValues(normalizedActionType, null, null, normalizedActionUrl);
    }

    private JsonNode normalizeActionParams(JsonNode actionParams) {
        if (actionParams == null || actionParams.isNull()) {
            return null;
        }
        if (!actionParams.isObject()) {
            throw validationError("actionParams는 JSON object여야 합니다.");
        }
        return actionParams.deepCopy();
    }

    private Map<String, Object> copyActionParams(JsonNode actionParams) {
        if (actionParams == null || actionParams.isNull()) {
            return null;
        }
        return ObjectMapperConfig.SHARED_OBJECT_MAPPER.convertValue(
                actionParams.deepCopy(),
                new TypeReference<Map<String, Object>>() {
                }
        );
    }

    private List<String> normalizeBannerIds(List<String> bannerIds) {
        return bannerIds.stream()
                .map(this::normalizeRequiredBannerId)
                .toList();
    }

    private void validateReorderIds(List<String> bannerIds, List<CampusBanner> orderedBanners) {
        Set<String> uniqueIds = new LinkedHashSet<>(bannerIds);
        if (uniqueIds.size() != bannerIds.size()) {
            throw validationError("bannerIds에는 중복 ID를 포함할 수 없습니다.");
        }

        Set<String> existingIds = orderedBanners.stream()
                .map(CampusBanner::getId)
                .collect(Collectors.toSet());

        String missingBannerId = bannerIds.stream()
                .filter(id -> !existingIds.contains(id))
                .findFirst()
                .orElse(null);
        if (missingBannerId != null) {
            throw new CampusBannerNotFoundException();
        }

        if (bannerIds.size() != orderedBanners.size()) {
            throw validationError("bannerIds는 전체 캠퍼스 배너 ID를 순서대로 모두 포함해야 합니다.");
        }
    }

    private void normalizeDisplayOrders(List<CampusBanner> orderedBanners) {
        int nextDisplayOrder = 1;
        for (CampusBanner orderedBanner : orderedBanners) {
            if (orderedBanner.getDisplayOrder() != nextDisplayOrder) {
                orderedBanner.changeDisplayOrder(nextDisplayOrder);
            }
            nextDisplayOrder++;
        }
    }

    private String normalizeRequiredText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw validationError(fieldName + "은 필수입니다.");
        }
        return trimmed;
    }

    private String normalizeRequiredBannerId(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw validationError("bannerIds 항목은 비어 있을 수 없습니다.");
        }
        return trimmed;
    }

    private boolean requireBoolean(Boolean value, String fieldName) {
        if (value == null) {
            throw validationError(fieldName + "는 필수입니다.");
        }
        return value;
    }

    private <T> T requireValue(T value, String fieldName) {
        if (value == null) {
            throw validationError(fieldName + "는 필수입니다.");
        }
        return value;
    }

    private BusinessException validationError(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private JsonNode copyJson(JsonNode source) {
        if (source == null || source.isNull()) {
            return null;
        }
        return source.deepCopy();
    }

    private record ActionValues(
            CampusBannerActionType actionType,
            CampusBannerActionTarget actionTarget,
            JsonNode actionParams,
            String actionUrl
    ) {
    }
}
