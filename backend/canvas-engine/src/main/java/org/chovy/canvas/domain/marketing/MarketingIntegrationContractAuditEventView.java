package org.chovy.canvas.domain.marketing;

import java.time.LocalDateTime;
import java.util.Map;

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
