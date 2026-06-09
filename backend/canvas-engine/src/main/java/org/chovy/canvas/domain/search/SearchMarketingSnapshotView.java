package org.chovy.canvas.domain.search;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * SearchMarketingSnapshotView 承载 domain.search 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param sourceId sourceId 字段。
 * @param keywordId keywordId 字段。
 * @param channel channel 字段。
 * @param snapshotDate snapshotDate 字段。
 * @param device device 字段。
 * @param country country 字段。
 * @param queryGroupKey queryGroupKey 字段。
 * @param impressionCount impressionCount 字段。
 * @param clickCount clickCount 字段。
 * @param costAmount costAmount 字段。
 * @param conversionCount conversionCount 字段。
 * @param revenueAmount revenueAmount 字段。
 * @param averagePosition averagePosition 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record SearchMarketingSnapshotView(
        Long id,
        Long tenantId,
        Long sourceId,
        Long keywordId,
        String channel,
        LocalDate snapshotDate,
        String device,
        String country,
        String queryGroupKey,
        Long impressionCount,
        Long clickCount,
        BigDecimal costAmount,
        Long conversionCount,
        BigDecimal revenueAmount,
        BigDecimal averagePosition,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public SearchMarketingSnapshotView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
