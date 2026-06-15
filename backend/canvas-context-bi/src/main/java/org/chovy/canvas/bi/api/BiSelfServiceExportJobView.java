package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;

public record BiSelfServiceExportJobView(
        Long id,
        Long tenantId,
        String resourceType,
        String resourceKey,
        Long resourceId,
        String exportFormat,
        Map<String, Object> query,
        int rowLimit,
        String status,
        String approvalStatus,
        String approvalReason,
        String reviewComment,
        String requestedBy,
        String reviewedBy,
        String processedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public BiSelfServiceExportJobView {
        query = query == null ? Map.of() : Map.copyOf(query);
    }
}
