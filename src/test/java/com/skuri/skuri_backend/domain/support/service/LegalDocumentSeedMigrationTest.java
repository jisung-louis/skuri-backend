package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.seed.repository.SeedMigrationRepository;
import com.skuri.skuri_backend.domain.support.repository.LegalDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalDocumentSeedMigrationTest {

    @Mock
    private LegalDocumentRepository legalDocumentRepository;

    @Mock
    private SeedMigrationRepository seedMigrationRepository;

    @InjectMocks
    private LegalDocumentSeedMigration legalDocumentSeedMigration;

    @Test
    void seed_이미적용된마이그레이션이면_재실행하지않는다() {
        when(seedMigrationRepository.existsById(LegalDocumentSeedMigration.MIGRATION_KEY)).thenReturn(true);

        legalDocumentSeedMigration.seed();

        verify(legalDocumentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void seed_최초실행이면_문서를적재하고_마커를기록한다() {
        when(seedMigrationRepository.existsById(LegalDocumentSeedMigration.MIGRATION_KEY)).thenReturn(false);
        when(legalDocumentRepository.existsById("termsOfUse")).thenReturn(false);
        when(legalDocumentRepository.existsById("privacyPolicy")).thenReturn(false);

        legalDocumentSeedMigration.seed();

        verify(legalDocumentRepository, times(2)).save(org.mockito.ArgumentMatchers.any());
        verify(seedMigrationRepository).save(org.mockito.ArgumentMatchers.any());
    }
}
