package org.chovy.canvas.domain.bi.export;

import java.time.LocalDateTime;

/**
 * BiExportJobView 承载 domain.bi.export 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workspaceId workspaceId 字段。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param resourceId resourceId 字段。
 * @param exportFormat exportFormat 字段。
 * @param rowLimit rowLimit 字段。
 * @param status status 字段。
 * @param progressPercent progressPercent 字段。
 * @param fileUrl fileUrl 字段。
 * @param storageProvider storageProvider 字段。
 * @param storageKey storageKey 字段。
 * @param retentionDays retentionDays 字段。
 * @param expiresAt expiresAt 字段。
 * @param downloadCount downloadCount 字段。
 * @param lastDownloadedAt lastDownloadedAt 字段。
 * @param approvalStatus approvalStatus 字段。
 * @param approvalReason approvalReason 字段。
 * @param requestedBy requestedBy 字段。
 * @param requestedAt requestedAt 字段。
 * @param reviewedBy reviewedBy 字段。
 * @param reviewedAt reviewedAt 字段。
 * @param reviewComment reviewComment 字段。
 * @param errorMessage errorMessage 字段。
 * @param retryCount retryCount 字段。
 * @param maxRetryCount maxRetryCount 字段。
 * @param nextRetryAt nextRetryAt 字段。
 * @param lastRetryAt lastRetryAt 字段。
 * @param retryExhaustedAt retryExhaustedAt 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
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
