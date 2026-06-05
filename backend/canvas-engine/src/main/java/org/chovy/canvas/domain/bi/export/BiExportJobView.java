package org.chovy.canvas.domain.bi.export;

import java.time.LocalDateTime;

public record BiExportJobView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        Long resourceId,
        String exportFormat,
        Integer rowLimit,
        String status,
        String fileUrl,
        String storageProvider,
        String storageKey,
        Integer retentionDays,
        LocalDateTime expiresAt,
        Integer downloadCount,
        LocalDateTime lastDownloadedAt,
        String approvalStatus,
        String approvalReason,
        String requestedBy,
        LocalDateTime requestedAt,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewComment,
        String errorMessage,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
