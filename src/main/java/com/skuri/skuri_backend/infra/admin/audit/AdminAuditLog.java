package com.skuri.skuri_backend.infra.admin.audit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "admin_audit_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "actor_id", length = 36, nullable = false)
    private String actorId;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "target_id", length = 36)
    private String targetId;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Convert(converter = JsonNodeJsonConverter.class)
    @Column(name = "diff_before", columnDefinition = "json")
    private JsonNode diffBefore;

    @Convert(converter = JsonNodeJsonConverter.class)
    @Column(name = "diff_after", columnDefinition = "json")
    private JsonNode diffAfter;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    private AdminAuditLog(
            String actorId,
            String action,
            String targetId,
            String targetType,
            JsonNode diffBefore,
            JsonNode diffAfter,
            LocalDateTime timestamp
    ) {
        this.id = UUID.randomUUID().toString();
        this.actorId = actorId;
        this.action = action;
        this.targetId = targetId;
        this.targetType = targetType;
        this.diffBefore = diffBefore;
        this.diffAfter = diffAfter;
        this.timestamp = timestamp;
    }

    public static AdminAuditLog create(
            String actorId,
            String action,
            String targetId,
            String targetType,
            JsonNode diffBefore,
            JsonNode diffAfter,
            LocalDateTime timestamp
    ) {
        return new AdminAuditLog(actorId, action, targetId, targetType, diffBefore, diffAfter, timestamp);
    }
}
