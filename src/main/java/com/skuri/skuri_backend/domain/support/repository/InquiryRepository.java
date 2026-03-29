package com.skuri.skuri_backend.domain.support.repository;

import com.skuri.skuri_backend.domain.support.entity.Inquiry;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, String> {

    long countByStatus(InquiryStatus status);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime start,
            LocalDateTime endExclusive
    );

    List<Inquiry> findAllByUserId(String userId);

    List<Inquiry> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<Inquiry> findByUserId(String userId, Pageable pageable);

    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Inquiry> findByStatusOrderByCreatedAtDesc(InquiryStatus status, Pageable pageable);
}
