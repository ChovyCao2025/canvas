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
        Integer progressPercent,
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
        Integer retryCount,
        Integer maxRetryCount,
        LocalDateTime nextRetryAt,
        LocalDateTime lastRetryAt,
        LocalDateTime retryExhaustedAt,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
