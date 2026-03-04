package com.skuri.skuri_backend.domain.taxiparty.repository;

import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequest;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, String> {

    boolean existsByParty_IdAndRequesterIdAndStatus(String partyId, String requesterId, JoinRequestStatus status);

    @EntityGraph(attributePaths = "party")
    Optional<JoinRequest> findDetailById(String id);

    List<JoinRequest> findByParty_IdOrderByCreatedAtDesc(String partyId);

    List<JoinRequest> findByRequesterIdOrderByCreatedAtDesc(String requesterId);

    List<JoinRequest> findByRequesterIdAndStatusOrderByCreatedAtDesc(String requesterId, JoinRequestStatus status);
}
