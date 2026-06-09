package org.chovy.canvas.domain.search;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * SearchMarketingProviderChangeView 承载 domain.search 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param sourceId sourceId 字段。
 * @param mutationId mutationId 字段。
 * @param provider provider 字段。
 * @param externalResourceId externalResourceId 字段。
 * @param changeType changeType 字段。
 * @param changedFields changedFields 字段。
 * @param providerActor providerActor 字段。
 * @param providerChangedAt providerChangedAt 字段。
 * @param reconciliationStatus reconciliationStatus 字段。
 * @param evidence evidence 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record SearchMarketingProviderChangeView(
        Long id,
        Long tenantId,
        Long sourceId,
        Long mutationId,
        String provider,
        String externalResourceId,
        String changeType,
        Map<String, Object> changedFields,
        String providerActor,
        LocalDateTime providerChangedAt,
        String reconciliationStatus,
        Map<String, Object> evidence,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public SearchMarketingProviderChangeView {
        changedFields = changedFields == null ? Map.of() : Map.copyOf(changedFields);
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
