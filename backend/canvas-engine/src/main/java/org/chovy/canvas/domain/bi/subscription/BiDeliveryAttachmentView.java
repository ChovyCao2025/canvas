package org.chovy.canvas.domain.bi.subscription;

import java.time.LocalDateTime;

public record BiDeliveryAttachmentView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String jobType,
        Long jobId,
        String jobKey,
        Long deliveryLogId,
        String resourceType,
        Long resourceId,
        String attachmentKey,
        String attachmentType,
        String fileName,
        String contentType,
        String fileUrl,
        String storageProvider,
        String storageKey,
        Long sizeBytes,
        Integer retentionDays,
        LocalDateTime expiresAt,
        Integer downloadCount,
        LocalDateTime lastDownloadedAt,
        String status,
        String errorMessage,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
