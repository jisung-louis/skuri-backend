package com.skuri.skuri_backend.domain.image.service;

import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileImageStorageServiceTest {

    @Mock
    private StorageRepository storageRepository;

    @InjectMocks
    private ProfileImageStorageService profileImageStorageService;

    @Test
    void deleteManagedProfileImage_프로필업로드URL이면_원본과썸네일후보를삭제한다() {
        when(storageRepository.resolveRelativePath("https://cdn.skuri.app/uploads/profiles/2026/04/06/photo.jpg"))
                .thenReturn(Optional.of("profiles/2026/04/06/photo.jpg"));

        profileImageStorageService.deleteManagedProfileImage("https://cdn.skuri.app/uploads/profiles/2026/04/06/photo.jpg");

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageRepository).resolveRelativePath("https://cdn.skuri.app/uploads/profiles/2026/04/06/photo.jpg");
        verify(storageRepository, org.mockito.Mockito.times(4)).delete(pathCaptor.capture());
        assertEquals(
                Set.of(
                        "profiles/2026/04/06/photo.jpg",
                        "profiles/2026/04/06/photo_thumb.jpg",
                        "profiles/2026/04/06/photo_thumb.png",
                        "profiles/2026/04/06/photo_thumb.webp"
                ),
                Set.copyOf(pathCaptor.getAllValues())
        );
        verifyNoMoreInteractions(storageRepository);
    }

    @Test
    void deleteManagedProfileImage_외부URL이면_스토리지삭제를호출하지않는다() {
        when(storageRepository.resolveRelativePath("https://images.example.com/profile.jpg"))
                .thenReturn(Optional.empty());

        profileImageStorageService.deleteManagedProfileImage("https://images.example.com/profile.jpg");

        verify(storageRepository).resolveRelativePath("https://images.example.com/profile.jpg");
        verify(storageRepository, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deleteManagedProfileImage_프로필컨텍스트가아닌내부URL이면_스토리지삭제를호출하지않는다() {
        when(storageRepository.resolveRelativePath("https://cdn.skuri.app/uploads/posts/2026/04/06/post.jpg"))
                .thenReturn(Optional.of("posts/2026/04/06/post.jpg"));

        profileImageStorageService.deleteManagedProfileImage("https://cdn.skuri.app/uploads/posts/2026/04/06/post.jpg");

        verify(storageRepository).resolveRelativePath("https://cdn.skuri.app/uploads/posts/2026/04/06/post.jpg");
        verify(storageRepository, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deleteManagedProfileImage_스토리지삭제예외는_삼키고_계속진행한다() {
        when(storageRepository.resolveRelativePath("https://cdn.skuri.app/uploads/profiles/2026/04/06/photo.jpg"))
                .thenReturn(Optional.of("profiles/2026/04/06/photo.jpg"));
        doThrow(new UncheckedIOException("boom", new java.io.IOException("boom")))
                .when(storageRepository)
                .delete("profiles/2026/04/06/photo.jpg");

        assertDoesNotThrow(() -> profileImageStorageService.deleteManagedProfileImage("https://cdn.skuri.app/uploads/profiles/2026/04/06/photo.jpg"));

        verify(storageRepository).delete("profiles/2026/04/06/photo.jpg");
        verify(storageRepository).delete("profiles/2026/04/06/photo_thumb.jpg");
        verify(storageRepository).delete("profiles/2026/04/06/photo_thumb.png");
        verify(storageRepository).delete("profiles/2026/04/06/photo_thumb.webp");
    }
}
