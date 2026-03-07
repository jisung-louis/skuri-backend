package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.support.dto.request.UpsertAppVersionRequest;
import com.skuri.skuri_backend.domain.support.dto.response.AppVersionAdminUpdateResponse;
import com.skuri.skuri_backend.domain.support.dto.response.AppVersionResponse;
import com.skuri.skuri_backend.domain.support.entity.AppPlatform;
import com.skuri.skuri_backend.domain.support.entity.AppVersion;
import com.skuri.skuri_backend.domain.support.exception.AppVersionNotFoundException;
import com.skuri.skuri_backend.domain.support.repository.AppVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AppVersionService {

    private final AppVersionRepository appVersionRepository;

    @Transactional(readOnly = true)
    public AppVersionResponse getAppVersion(String platform) {
        AppVersion appVersion = appVersionRepository.findById(normalizePlatform(platform))
                .orElseThrow(AppVersionNotFoundException::new);
        return toResponse(appVersion);
    }

    @Transactional
    public AppVersionAdminUpdateResponse upsertAppVersion(String platform, UpsertAppVersionRequest request) {
        String normalizedPlatform = normalizePlatform(platform);
        AppVersion appVersion = appVersionRepository.findById(normalizedPlatform).orElse(null);
        if (appVersion == null) {
            appVersion = appVersionRepository.saveAndFlush(AppVersion.create(
                    normalizedPlatform,
                    normalizeRequired(request.minimumVersion()),
                    request.forceUpdate(),
                    trimToNull(request.message()),
                    trimToNull(request.title()),
                    request.showButton(),
                    normalizeButtonField(request.showButton(), request.buttonText()),
                    normalizeButtonField(request.showButton(), request.buttonUrl())
            ));
            return toAdminResponse(appVersion);
        }

        appVersion.update(
                normalizeRequired(request.minimumVersion()),
                request.forceUpdate(),
                trimToNull(request.message()),
                trimToNull(request.title()),
                request.showButton(),
                normalizeButtonField(request.showButton(), request.buttonText()),
                normalizeButtonField(request.showButton(), request.buttonUrl())
        );
        appVersionRepository.saveAndFlush(appVersion);
        return toAdminResponse(appVersion);
    }

    private AppVersionResponse toResponse(AppVersion appVersion) {
        return new AppVersionResponse(
                appVersion.getPlatform(),
                appVersion.getMinimumVersion(),
                appVersion.isForceUpdate(),
                appVersion.getMessage(),
                appVersion.getTitle(),
                appVersion.isShowButton(),
                appVersion.getButtonText(),
                appVersion.getButtonUrl()
        );
    }

    private AppVersionAdminUpdateResponse toAdminResponse(AppVersion appVersion) {
        return new AppVersionAdminUpdateResponse(
                appVersion.getPlatform(),
                appVersion.getMinimumVersion(),
                appVersion.isForceUpdate(),
                appVersion.getUpdatedAt()
        );
    }

    private String normalizePlatform(String platform) {
        try {
            return AppPlatform.from(platform).value();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, e.getMessage());
        }
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeButtonField(boolean showButton, String value) {
        if (!showButton) {
            return null;
        }
        return normalizeRequired(value);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
