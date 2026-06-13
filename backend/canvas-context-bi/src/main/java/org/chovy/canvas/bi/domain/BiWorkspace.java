package org.chovy.canvas.bi.domain;

import java.time.LocalDateTime;

public record BiWorkspace(
        Long id,
        Long tenantId,
        BiResourceKey workspaceKey,
        String name,
        String description,
        BiResourceStatus status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public BiWorkspace {
        tenantId = tenantId == null ? 0L : tenantId;
        workspaceKey = workspaceKey == null ? BiResourceKey.of(name, "workspaceKey") : workspaceKey;
        name = textOrDefault(name, workspaceKey.value());
        status = status == null ? BiResourceStatus.DRAFT : status;
    }

    public BiWorkspace withId(Long newId) {
        return new BiWorkspace(newId, tenantId, workspaceKey, name, description, status, createdBy, createdAt, updatedAt);
    }

    private static String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
