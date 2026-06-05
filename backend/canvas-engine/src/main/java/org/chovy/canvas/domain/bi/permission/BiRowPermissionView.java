package org.chovy.canvas.domain.bi.permission;

import java.time.LocalDateTime;

public record BiRowPermissionView(
        Long id,
        Long tenantId,
        String datasetKey,
        Long datasetId,
        String ruleKey,
        String subjectType,
        String subjectId,
        String filterJson,
        boolean enabled,
        LocalDateTime createdAt) {
}
