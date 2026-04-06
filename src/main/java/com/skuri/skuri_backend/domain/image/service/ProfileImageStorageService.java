package com.skuri.skuri_backend.domain.image.service;

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

    private final StorageRepository storageRepository;

    public void deleteManagedProfileImage(String photoUrl) {
        if (!StringUtils.hasText(photoUrl)) {
            return;
        }

        storageRepository.resolveRelativePath(photoUrl)
                .filter(this::isManagedProfileImagePath)
                .ifPresent(relativePath -> buildDeletionCandidates(relativePath).forEach(this::deleteQuietly));
    }

    private boolean isManagedProfileImagePath(String relativePath) {
        return StringUtils.hasText(relativePath) && relativePath.replace('\\', '/').startsWith(PROFILE_DIRECTORY_PREFIX);
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
