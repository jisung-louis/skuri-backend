package com.skuri.skuri_backend.domain.image.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.image.dto.request.ImageUploadContext;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImageStorageService {

    private static final String PROFILE_DIRECTORY_PREFIX = ImageUploadContext.PROFILE_IMAGE.directoryName() + "/";
    private static final String THUMB_SUFFIX = "_thumb";
    private static final List<String> THUMBNAIL_EXTENSIONS = List.of("jpg", "png", "webp");
    private static final String PROFILE_IMAGE_OWNERSHIP_MESSAGE =
            "photoUrl은 본인이 업로드한 PROFILE_IMAGE URL만 사용할 수 있습니다.";

    private final StorageRepository storageRepository;

    public void validateProfilePhotoReference(String memberId, String currentPhotoUrl, String requestedPhotoUrl) {
        if (!StringUtils.hasText(requestedPhotoUrl)) {
            return;
        }
        if (requestedPhotoUrl.equals(currentPhotoUrl)) {
            return;
        }

        storageRepository.resolveRelativePath(requestedPhotoUrl)
                .ifPresent(relativePath -> {
                    if (!isOwnedManagedProfileImagePath(memberId, relativePath)) {
                        throw new BusinessException(ErrorCode.VALIDATION_ERROR, PROFILE_IMAGE_OWNERSHIP_MESSAGE);
                    }
                });
    }

    public void deleteOwnedManagedProfileImage(String memberId, String photoUrl) {
        if (!StringUtils.hasText(photoUrl)) {
            return;
        }

        storageRepository.resolveRelativePath(photoUrl)
                .filter(relativePath -> isOwnedManagedProfileImagePath(memberId, relativePath))
                .ifPresent(relativePath -> buildDeletionCandidates(relativePath).forEach(this::deleteQuietly));
    }

    private boolean isManagedProfileImagePath(String relativePath) {
        return StringUtils.hasText(relativePath) && relativePath.replace('\\', '/').startsWith(PROFILE_DIRECTORY_PREFIX);
    }

    private boolean isOwnedManagedProfileImagePath(String memberId, String relativePath) {
        if (!StringUtils.hasText(memberId) || !isManagedProfileImagePath(relativePath)) {
            return false;
        }

        String normalizedPath = relativePath.replace('\\', '/');
        String ownedPrefix = PROFILE_DIRECTORY_PREFIX + memberId.trim() + "/";
        return normalizedPath.startsWith(ownedPrefix) && normalizedPath.split("/").length >= 6;
    }

    private Set<String> buildDeletionCandidates(String relativePath) {
        String normalizedPath = relativePath.replace('\\', '/');
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(normalizedPath);

        int extensionSeparator = normalizedPath.lastIndexOf('.');
        if (extensionSeparator < 0) {
            return candidates;
        }

        String fileNameWithoutExtension = normalizedPath.substring(0, extensionSeparator);
        if (!StringUtils.hasText(fileNameWithoutExtension)) {
            return candidates;
        }

        String thumbBasePath = fileNameWithoutExtension.endsWith(THUMB_SUFFIX)
                ? fileNameWithoutExtension
                : fileNameWithoutExtension + THUMB_SUFFIX;

        for (String extension : THUMBNAIL_EXTENSIONS) {
            candidates.add(thumbBasePath + "." + extension);
        }
        return candidates;
    }

    private void deleteQuietly(String relativePath) {
        try {
            storageRepository.delete(relativePath);
        } catch (RuntimeException e) {
            log.warn("프로필 이미지 정리 실패: path={}, message={}", relativePath, e.getMessage(), e);
        }
    }
}
