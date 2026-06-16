package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;
/**
 * BiSelfServiceExportJobView 视图。
 */
public record BiSelfServiceExportJobView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        /**
         * 资源标识。
         */
        Long resourceId,
        /**
         * exportFormat 对应的时间。
         */
        String exportFormat,
        /**
         * 查询定义。
         */
        Map<String, Object> query,
        /**
         * rowLimit 字段值。
         */
        int rowLimit,
        /**
         * 状态值。
         */
        String status,
        /**
         * approvalStatus 对应的数据集合。
         */
        String approvalStatus,
        /**
         * approvalReason 字段值。
         */
        String approvalReason,
        /**
         * reviewComment 字段值。
         */
        String reviewComment,
        /**
         * requestedBy 字段值。
         */
        String requestedBy,
        /**
         * reviewedBy 字段值。
         */
        String reviewedBy,
        /**
         * processedBy 字段值。
         */
        String processedBy,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public BiSelfServiceExportJobView {
        query = query == null ? Map.of() : Map.copyOf(query);
    }
}
