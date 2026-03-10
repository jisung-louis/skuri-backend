package com.skuri.skuri_backend.infra.admin.audit;

import lombok.Getter;

@Getter
public class AdminAuditRequestContext {

    private final String action;
    private final String targetType;
    private final String targetIdExpression;
    private final String beforeExpression;
    private final String afterExpression;
    private String actorId;
    private Object requestBody;
    private String targetId;
    private Object beforeSnapshot;
    private boolean beforePrepared;

    public AdminAuditRequestContext(AdminAudit adminAudit) {
        this.action = adminAudit.action();
        this.targetType = adminAudit.targetType();
        this.targetIdExpression = adminAudit.targetId();
        this.beforeExpression = adminAudit.before();
        this.afterExpression = adminAudit.after();
    }

    public void setRequestBody(Object requestBody) {
        this.requestBody = requestBody;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public void updateTargetId(String targetId) {
        if (targetId != null && !targetId.isBlank()) {
            this.targetId = targetId;
        }
    }

    public void markBeforePrepared(Object beforeSnapshot) {
        this.beforePrepared = true;
        this.beforeSnapshot = beforeSnapshot;
    }

    public boolean hasBeforeExpression() {
        return beforeExpression != null && !beforeExpression.isBlank();
    }

    public boolean hasAfterExpression() {
        return afterExpression != null && !afterExpression.isBlank();
    }
}
