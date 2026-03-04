package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyTimeoutBatchService {

    private final PartyRepository partyRepository;
    private final PartyTimeoutCommandService partyTimeoutCommandService;

    @Transactional(readOnly = true)
    public PartyTimeoutBatchResult endExpiredParties() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(12);
        List<String> targetIds = partyRepository.findTimeoutTargetIds(threshold);

        int ended = 0;
        int conflicted = 0;
        int skipped = 0;

        for (String partyId : targetIds) {
            try {
                boolean changed = partyTimeoutCommandService.endExpiredParty(partyId);
                if (changed) {
                    ended++;
                } else {
                    skipped++;
                }
            } catch (BusinessException e) {
                if (e.getErrorCode() == ErrorCode.PARTY_CONCURRENT_MODIFICATION) {
                    conflicted++;
                    log.warn("파티 자동 종료 충돌: partyId={}", partyId);
                    continue;
                }
                throw e;
            }
        }

        return new PartyTimeoutBatchResult(targetIds.size(), ended, conflicted, skipped);
    }
}
