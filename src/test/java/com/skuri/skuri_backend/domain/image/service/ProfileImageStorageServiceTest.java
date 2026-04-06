package com.skuri.skuri_backend.domain.image.service;

import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileImageStorageServiceTest {

    private static final String MEMBER_ID = "firebase-uid";
    private static final String OWNED_PROFILE_URL =
            "https://cdn.skuri.app/uploads/profiles/firebase-uid/2026/04/06/photo.jpg";
    private static final String OWNED_PROFILE_PATH = "profiles/firebase-uid/2026/04/06/photo.jpg";
    private static final String PROFILE_IMAGE_OWNERSHIP_MESSAGE =
            "photoUrl은 본인이 업로드한 PROFILE_IMAGE URL만 사용할 수 있습니다.";

    @Mock
    private StorageRepository storageRepository;

    @InjectMocks
    private ProfileImageStorageService profileImageStorageService;

    @Test
    void validateProfilePhotoReference_본인소유내부PROFILE_IMAGE면_허용한다() {
        when(storageRepository.resolveRelativePath(OWNED_PROFILE_URL))
                .thenReturn(Optional.of(OWNED_PROFILE_PATH));

        assertDoesNotThrow(() -> profileImageStorageService.validateProfilePhotoReference(
                MEMBER_ID,
                "https://example.com/old.jpg",
                OWNED_PROFILE_URL
        ));
    }

    @Test
    void validateProfilePhotoReference_현재값과같은레거시내부URL이면_허용한다() {
        assertDoesNotThrow(() -> profileImageStorageService.validateProfilePhotoReference(
                MEMBER_ID,
                "https://cdn.skuri.app/uploads/profiles/2026/04/06/photo.jpg",
                "https://cdn.skuri.app/uploads/profiles/2026/04/06/photo.jpg"
        ));
        verify(storageRepository, never()).resolveRelativePath("https://cdn.skuri.app/uploads/profiles/2026/04/06/photo.jpg");
    }

    @Test
    void validateProfilePhotoReference_타인내부프로필URL이면_422를던진다() {
        when(storageRepository.resolveRelativePath("https://cdn.skuri.app/uploads/profiles/other-member/2026/04/06/photo.jpg"))
                .thenReturn(Optional.of("profiles/other-member/2026/04/06/photo.jpg"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> profileImageStorageService.validateProfilePhotoReference(
                        MEMBER_ID,
                        "https://example.com/old.jpg",
                        "https://cdn.skuri.app/uploads/profiles/other-member/2026/04/06/photo.jpg"
                )
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals(PROFILE_IMAGE_OWNERSHIP_MESSAGE, exception.getMessage());
    }

    @Test
    void deleteOwnedManagedProfileImage_본인프로필업로드URL이면_원본과썸네일후보를삭제한다() {
        when(storageRepository.resolveRelativePath(OWNED_PROFILE_URL))
                .thenReturn(Optional.of(OWNED_PROFILE_PATH));

        profileImageStorageService.deleteOwnedManagedProfileImage(MEMBER_ID, OWNED_PROFILE_URL);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageRepository).resolveRelativePath(OWNED_PROFILE_URL);
        verify(storageRepository, org.mockito.Mockito.times(4)).delete(pathCaptor.capture());
        assertEquals(
                Set.of(
                        "profiles/firebase-uid/2026/04/06/photo.jpg",
                        "profiles/firebase-uid/2026/04/06/photo_thumb.jpg",
                        "profiles/firebase-uid/2026/04/06/photo_thumb.png",
                        "profiles/firebase-uid/2026/04/06/photo_thumb.webp"
                ),
                Set.copyOf(pathCaptor.getAllValues())
        );
        verifyNoMoreInteractions(storageRepository);
    }

    @Test
    void deleteOwnedManagedProfileImage_외부URL이면_스토리지삭제를호출하지않는다() {
        when(storageRepository.resolveRelativePath("https://images.example.com/profile.jpg"))
                .thenReturn(Optional.empty());

        profileImageStorageService.deleteOwnedManagedProfileImage(MEMBER_ID, "https://images.example.com/profile.jpg");

        verify(storageRepository).resolveRelativePath("https://images.example.com/profile.jpg");
        verify(storageRepository, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deleteOwnedManagedProfileImage_레거시내부프로필URL이면_스토리지삭제를호출하지않는다() {
        when(storageRepository.resolveRelativePath("https://cdn.skuri.app/uploads/profiles/2026/04/06/photo.jpg"))
                .thenReturn(Optional.of("profiles/2026/04/06/photo.jpg"));

        profileImageStorageService.deleteOwnedManagedProfileImage(MEMBER_ID, "https://cdn.skuri.app/uploads/profiles/2026/04/06/photo.jpg");

        verify(storageRepository).resolveRelativePath("https://cdn.skuri.app/uploads/profiles/2026/04/06/photo.jpg");
        verify(storageRepository, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deleteOwnedManagedProfileImage_프로필컨텍스트가아닌내부URL이면_스토리지삭제를호출하지않는다() {
        when(storageRepository.resolveRelativePath("https://cdn.skuri.app/uploads/posts/2026/04/06/post.jpg"))
                .thenReturn(Optional.of("posts/2026/04/06/post.jpg"));

        profileImageStorageService.deleteOwnedManagedProfileImage(MEMBER_ID, "https://cdn.skuri.app/uploads/posts/2026/04/06/post.jpg");

        verify(storageRepository).resolveRelativePath("https://cdn.skuri.app/uploads/posts/2026/04/06/post.jpg");
        verify(storageRepository, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deleteOwnedManagedProfileImage_스토리지삭제예외는_삼키고_계속진행한다() {
        when(storageRepository.resolveRelativePath(OWNED_PROFILE_URL))
                .thenReturn(Optional.of(OWNED_PROFILE_PATH));
        doThrow(new UncheckedIOException("boom", new java.io.IOException("boom")))
                .when(storageRepository)
                .delete("profiles/firebase-uid/2026/04/06/photo.jpg");

        assertDoesNotThrow(() -> profileImageStorageService.deleteOwnedManagedProfileImage(MEMBER_ID, OWNED_PROFILE_URL));

        verify(storageRepository).delete("profiles/firebase-uid/2026/04/06/photo.jpg");
        verify(storageRepository).delete("profiles/firebase-uid/2026/04/06/photo_thumb.jpg");
        verify(storageRepository).delete("profiles/firebase-uid/2026/04/06/photo_thumb.png");
        verify(storageRepository).delete("profiles/firebase-uid/2026/04/06/photo_thumb.webp");
    }
}
