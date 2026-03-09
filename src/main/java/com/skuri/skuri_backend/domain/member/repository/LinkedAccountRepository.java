package com.skuri.skuri_backend.domain.member.repository;

import com.skuri.skuri_backend.domain.member.entity.LinkedAccount;
import com.skuri.skuri_backend.domain.member.entity.LinkedAccountProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, Long> {

    boolean existsByMemberIdAndProvider(String memberId, LinkedAccountProvider provider);

    long deleteByMemberId(String memberId);
}
