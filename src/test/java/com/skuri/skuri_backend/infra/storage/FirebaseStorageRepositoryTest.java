package com.skuri.skuri_backend.infra.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static com.skuri.skuri_backend.infra.storage.FirebaseStorageRepository.DOWNLOAD_TOKEN_METADATA_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FirebaseStorageRepositoryTest {

    @Test
    void store_다운로드토큰과Firebase다운로드URL을생성한다() {
        Bucket bucket = mock(Bucket.class);
        Storage storage = mock(Storage.class);
        Blob blob = mock(Blob.class);
        byte[] imageData = "image-data".getBytes();
        when(bucket.getName()).thenReturn("sktaxi-acb4c.firebasestorage.app");
        when(bucket.getStorage()).thenReturn(storage);
        when(storage.create(any(com.google.cloud.storage.BlobInfo.class), any(byte[].class))).thenReturn(blob);

        FirebaseStorageRepository repository = new FirebaseStorageRepository(bucket);

        StorageRepository.StoredObject storedObject = repository.store("posts/2026/03/10/image.jpg", imageData, "image/jpeg");

        ArgumentCaptor<com.google.cloud.storage.BlobInfo> blobInfoCaptor =
                ArgumentCaptor.forClass(com.google.cloud.storage.BlobInfo.class);
        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(storage).create(blobInfoCaptor.capture(), dataCaptor.capture());

        com.google.cloud.storage.BlobInfo blobInfo = blobInfoCaptor.getValue();
        assertEquals("image-data", new String(dataCaptor.getValue()));
        assertEquals("posts/2026/03/10/image.jpg", blobInfo.getName());
        assertEquals("image/jpeg", blobInfo.getContentType());
        String downloadToken = blobInfo.getMetadata().get(DOWNLOAD_TOKEN_METADATA_KEY);
        assertNotNull(downloadToken);
        assertEquals(UUID.fromString(downloadToken).toString(), downloadToken);
        assertEquals("posts/2026/03/10/image.jpg", storedObject.relativePath());
        assertTrue(
                storedObject.publicUrl().startsWith(
                        "https://firebasestorage.googleapis.com/v0/b/sktaxi-acb4c.firebasestorage.app/o/posts%2F2026%2F03%2F10%2Fimage.jpg?alt=media&token="
                )
        );
        assertTrue(storedObject.publicUrl().endsWith(downloadToken));
    }

    @Test
    void delete_파일이있으면삭제한다() {
        Bucket bucket = mock(Bucket.class);
        Blob blob = mock(Blob.class);
        when(bucket.get("chat/2026/03/10/image.png")).thenReturn(blob);

        FirebaseStorageRepository repository = new FirebaseStorageRepository(bucket);

        repository.delete("chat/2026/03/10/image.png");

        verify(blob).delete();
    }

    @Test
    void delete_파일이없으면무시한다() {
        Bucket bucket = mock(Bucket.class);
        when(bucket.get("profiles/2026/03/10/image.jpg")).thenReturn(null);

        FirebaseStorageRepository repository = new FirebaseStorageRepository(bucket);

        repository.delete("profiles/2026/03/10/image.jpg");

        verify(bucket).get("profiles/2026/03/10/image.jpg");
    }
}
