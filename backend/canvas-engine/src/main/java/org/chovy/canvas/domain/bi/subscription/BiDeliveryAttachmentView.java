package org.chovy.canvas.domain.bi.subscription;

import java.time.LocalDateTime;

/**
 * BiDeliveryAttachmentView 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workspaceId workspaceId 字段。
 * @param jobType jobType 字段。
 * @param jobId jobId 字段。
 * @param jobKey jobKey 字段。
 * @param deliveryLogId deliveryLogId 字段。
 * @param resourceType resourceType 字段。
 * @param resourceId resourceId 字段。
 * @param attachmentKey attachmentKey 字段。
 * @param attachmentType attachmentType 字段。
 * @param fileName fileName 字段。
 * @param contentType contentType 字段。
 * @param fileUrl fileUrl 字段。
 * @param storageProvider storageProvider 字段。
 * @param storageKey storageKey 字段。
 * @param sizeBytes sizeBytes 字段。
 * @param retentionDays retentionDays 字段。
 * @param expiresAt expiresAt 字段。
 * @param downloadCount downloadCount 字段。
 * @param lastDownloadedAt lastDownloadedAt 字段。
 * @param status status 字段。
 * @param errorMessage errorMessage 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
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
