package com.skuri.skuri_backend.infra.admin.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, String> {
}
