package com.skuri.skuri_backend.domain.support.repository;

import com.skuri.skuri_backend.domain.support.entity.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, String> {

    Optional<LegalDocument> findByDocumentKeyAndIsActiveTrue(String documentKey);

    List<LegalDocument> findAllByOrderByDocumentKeyAsc();
}
