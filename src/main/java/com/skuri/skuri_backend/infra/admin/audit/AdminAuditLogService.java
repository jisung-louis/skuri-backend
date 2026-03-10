package com.skuri.skuri_backend.infra.admin.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private static final ObjectMapper OBJECT_MAPPER = AdminAuditObjectMapper.OBJECT_MAPPER;

    private final AdminAuditLogRepository adminAuditLogRepository;

    @Transactional
    public void record(AdminAuditRequestContext context, Object afterSnapshot) {
        if (context.getActorId() == null) {
            return;
        }

        adminAuditLogRepository.save(AdminAuditLog.create(
                context.getActorId(),
                context.getAction(),
                context.getTargetId(),
                context.getTargetType(),
                toJsonNode(context.getBeforeSnapshot()),
                toJsonNode(afterSnapshot),
                LocalDateTime.now()
        ));
    }

    private JsonNode toJsonNode(Object source) {
        if (source == null) {
            return null;
        }
        return OBJECT_MAPPER.valueToTree(source);
    }
}
