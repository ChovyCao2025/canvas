package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MarketingIntegrationContractAuditEventView 承载 domain.marketing 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param contractId contractId 字段。
 * @param contractKey contractKey 字段。
 * @param revision revision 字段。
 * @param eventType eventType 字段。
 * @param previousStatus previousStatus 字段。
 * @param newStatus newStatus 字段。
 * @param snapshot snapshot 字段。
 * @param changedFields changedFields 字段。
 * @param changedBy changedBy 字段。
 * @param createdAt createdAt 字段。
 */
public record MarketingIntegrationContractAuditEventView(
        Long id,
        Long tenantId,
        Long contractId,
        String contractKey,
        Integer revision,
        String eventType,
        String previousStatus,
        String newStatus,
        Map<String, Object> snapshot,
        Map<String, Object> changedFields,
        String changedBy,
        LocalDateTime createdAt) {
}
