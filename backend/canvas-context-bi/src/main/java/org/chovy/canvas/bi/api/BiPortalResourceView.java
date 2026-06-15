package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record BiPortalResourceView(
        Long tenantId,
        String portalKey,
        String title,
        String description,
        List<String> dashboardKeys,
        Map<String, Object> layout,
        Map<String, Object> settings,
        String status,
        Integer version,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
