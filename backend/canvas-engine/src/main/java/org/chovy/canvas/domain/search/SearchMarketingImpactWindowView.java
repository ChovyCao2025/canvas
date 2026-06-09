package org.chovy.canvas.domain.search;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * SearchMarketingImpactWindowView 承载 domain.search 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param opportunityId opportunityId 字段。
 * @param mutationId mutationId 字段。
 * @param sourceId sourceId 字段。
 * @param keywordId keywordId 字段。
 * @param pageUrlHash pageUrlHash 字段。
 * @param baselineStartDate baselineStartDate 字段。
 * @param baselineEndDate baselineEndDate 字段。
 * @param postStartDate postStartDate 字段。
 * @param postEndDate postEndDate 字段。
 * @param status status 字段。
 * @param decision decision 字段。
 * @param confidence confidence 字段。
 * @param metricDeltas metricDeltas 字段。
 * @param evidence evidence 字段。
 * @param dueAt dueAt 字段。
 * @param evaluatedAt evaluatedAt 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record SearchMarketingImpactWindowView(
        Long id,
        Long tenantId,
        Long opportunityId,
        Long mutationId,
        Long sourceId,
        Long keywordId,
        String pageUrlHash,
        LocalDate baselineStartDate,
        LocalDate baselineEndDate,
        LocalDate postStartDate,
        LocalDate postEndDate,
        String status,
        String decision,
        BigDecimal confidence,
        Map<String, Object> metricDeltas,
        Map<String, Object> evidence,
        LocalDateTime dueAt,
        LocalDateTime evaluatedAt,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public SearchMarketingImpactWindowView {
        metricDeltas = metricDeltas == null ? Map.of() : Map.copyOf(metricDeltas);
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
