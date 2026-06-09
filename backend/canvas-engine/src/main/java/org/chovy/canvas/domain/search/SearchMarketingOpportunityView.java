package org.chovy.canvas.domain.search;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * SearchMarketingOpportunityView 承载 domain.search 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param sourceId sourceId 字段。
 * @param keywordId keywordId 字段。
 * @param channel channel 字段。
 * @param opportunityType opportunityType 字段。
 * @param snapshotDate snapshotDate 字段。
 * @param severity severity 字段。
 * @param status status 字段。
 * @param recommendation recommendation 字段。
 * @param impactScore impactScore 字段。
 * @param evidence evidence 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record SearchMarketingOpportunityView(
        Long id,
        Long tenantId,
        Long sourceId,
        Long keywordId,
        String channel,
        String opportunityType,
        LocalDate snapshotDate,
        String severity,
        String status,
        String recommendation,
        BigDecimal impactScore,
        Map<String, Object> evidence,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public SearchMarketingOpportunityView {
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
