package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class MemberStatusBackfillService {

    private final MemberRepository memberRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillLegacyMembersAsActive() {
        int updatedCount = memberRepository.backfillLegacyMembersAsActive(MemberStatus.ACTIVE);
        if (updatedCount > 0) {
            log.info("기존 회원 status backfill 완료: {}건", updatedCount);
        }
    }
}
