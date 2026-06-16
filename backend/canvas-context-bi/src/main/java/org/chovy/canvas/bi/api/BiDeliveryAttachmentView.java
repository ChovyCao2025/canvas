package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiDeliveryAttachmentView 视图。
 */
public record BiDeliveryAttachmentView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 工作空间标识。
         */
        Long workspaceId,
        /**
         * jobType 字段值。
         */
        String jobType,
        /**
         * jobId 对应的标识。
         */
        Long jobId,
        /**
         * jobKey 对应的业务键。
         */
        String jobKey,
        /**
         * deliveryLogId 对应的标识。
         */
        Long deliveryLogId,
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源标识。
         */
        Long resourceId,
        /**
         * attachmentKey 对应的业务键。
         */
        String attachmentKey,
        /**
         * attachmentType 字段值。
         */
        String attachmentType,
        /**
         * fileName 字段值。
         */
        String fileName,
        /**
         * 内容类型。
         */
        String contentType,
        /**
         * fileUrl 字段值。
         */
        String fileUrl,
        /**
         * storageProvider 字段值。
         */
        String storageProvider,
        /**
         * storageKey 对应的业务键。
         */
        String storageKey,
        /**
         * sizeBytes 对应的数据集合。
         */
        Long sizeBytes,
        /**
         * retentionDays 对应的数据集合。
         */
        Integer retentionDays,
        /**
         * expiresAt 对应的时间。
         */
        LocalDateTime expiresAt,
        /**
         * downloadCount 对应的统计数量。
         */
        Integer downloadCount,
        /**
         * lastDownloadedAt 对应的时间。
         */
        LocalDateTime lastDownloadedAt,
        /**
         * 状态值。
         */
        String status,
        /**
         * errorMessage 字段值。
         */
        String errorMessage,
        /**
         * 创建人。
         */
        String createdBy,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
