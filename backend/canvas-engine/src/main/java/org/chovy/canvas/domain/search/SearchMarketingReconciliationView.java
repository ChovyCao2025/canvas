package org.chovy.canvas.domain.search;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * SearchMarketingReconciliationView 承载 domain.search 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param mutationId mutationId 字段。
 * @param providerChangeId providerChangeId 字段。
 * @param status status 字段。
 * @param providerOperationId providerOperationId 字段。
 * @param evidence evidence 字段。
 * @param reconciledAt reconciledAt 字段。
 */
public record SearchMarketingReconciliationView(
        Long tenantId,
        Long mutationId,
        Long providerChangeId,
        String status,
        String providerOperationId,
        Map<String, Object> evidence,
        LocalDateTime reconciledAt) {

    public SearchMarketingReconciliationView {
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
