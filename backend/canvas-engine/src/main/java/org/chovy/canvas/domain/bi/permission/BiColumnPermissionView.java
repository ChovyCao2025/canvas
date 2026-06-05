package org.chovy.canvas.domain.bi.permission;

import java.time.LocalDateTime;

public record BiColumnPermissionView(
        Long id,
        Long tenantId,
        String datasetKey,
        Long datasetId,
        String fieldKey,
        String subjectType,
        String subjectId,
        String policy,
        String maskJson,
        boolean enabled,
        LocalDateTime createdAt) {
}
