package org.chovy.canvas.domain.bi.export;

import org.chovy.canvas.domain.bi.query.BiQueryRequest;

/**
 * BiExportJobCommand 承载 domain.bi.export 场景中的不可变数据快照。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param resourceId resourceId 字段。
 * @param exportFormat exportFormat 字段。
 * @param query query 字段。
 * @param rowLimit rowLimit 字段。
 * @param approvalRequired approvalRequired 字段。
 * @param sensitive sensitive 字段。
 * @param approvalReason approvalReason 字段。
 */
public record BiExportJobCommand(
        String resourceType,
        String resourceKey,
        Long resourceId,
        String exportFormat,
        BiQueryRequest query,
        Integer rowLimit,
        Boolean approvalRequired,
        Boolean sensitive,
        String approvalReason) {
}
