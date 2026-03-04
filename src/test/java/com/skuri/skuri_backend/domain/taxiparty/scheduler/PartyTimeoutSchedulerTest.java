package com.skuri.skuri_backend.domain.taxiparty.scheduler;

import com.skuri.skuri_backend.domain.taxiparty.service.PartyTimeoutBatchResult;
import com.skuri.skuri_backend.domain.taxiparty.service.PartyTimeoutBatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyTimeoutSchedulerTest {

    @Mock
    private PartyTimeoutBatchService partyTimeoutBatchService;

    @InjectMocks
    private PartyTimeoutScheduler partyTimeoutScheduler;

    @Test
    void endExpiredParties_스케줄러가서비스를호출한다() {
        when(partyTimeoutBatchService.endExpiredParties())
                .thenReturn(new PartyTimeoutBatchResult(3, 2, 1, 0));

        partyTimeoutScheduler.endExpiredParties();

        verify(partyTimeoutBatchService).endExpiredParties();
    }
}
