package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberStatusBackfillServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberStatusBackfillService memberStatusBackfillService;

    @Test
    void backfillLegacyMembersAsActive_기존회원status를_ACTIVE로백필한다() {
        when(memberRepository.backfillLegacyMembersAsActive(MemberStatus.ACTIVE)).thenReturn(3);

        memberStatusBackfillService.backfillLegacyMembersAsActive();

        verify(memberRepository).backfillLegacyMembersAsActive(MemberStatus.ACTIVE);
    }
}
