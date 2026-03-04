package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyTimeoutBatchServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private PartyTimeoutCommandService partyTimeoutCommandService;

    @InjectMocks
    private PartyTimeoutBatchService partyTimeoutBatchService;

    @Test
    void endExpiredParties_중간충돌이있어도_나머지계속처리() {
        when(partyRepository.findTimeoutTargetIds(any(LocalDateTime.class)))
                .thenReturn(List.of("party-1", "party-2", "party-3"));

        when(partyTimeoutCommandService.endExpiredParty("party-1")).thenReturn(true);
        doThrow(new BusinessException(ErrorCode.PARTY_CONCURRENT_MODIFICATION))
                .when(partyTimeoutCommandService).endExpiredParty("party-2");
        when(partyTimeoutCommandService.endExpiredParty("party-3")).thenReturn(false);

        PartyTimeoutBatchResult result = partyTimeoutBatchService.endExpiredParties();

        assertEquals(3, result.targetCount());
        assertEquals(1, result.endedCount());
        assertEquals(1, result.conflictedCount());
        assertEquals(1, result.skippedCount());

        verify(partyTimeoutCommandService).endExpiredParty("party-1");
        verify(partyTimeoutCommandService).endExpiredParty("party-2");
        verify(partyTimeoutCommandService).endExpiredParty("party-3");
    }
}
