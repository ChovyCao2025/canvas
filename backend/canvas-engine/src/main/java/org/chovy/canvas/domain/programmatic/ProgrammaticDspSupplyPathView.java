package org.chovy.canvas.domain.programmatic;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ProgrammaticDspSupplyPathView 承载 domain.programmatic 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param lineItemId lineItemId 字段。
 * @param exchangeKey exchangeKey 字段。
 * @param dealId dealId 字段。
 * @param sellerId sellerId 字段。
 * @param sellerDomain sellerDomain 字段。
 * @param inventoryType inventoryType 字段。
 * @param adsTxtStatus adsTxtStatus 字段。
 * @param sellersJsonStatus sellersJsonStatus 字段。
 * @param schainComplete schainComplete 字段。
 * @param status status 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record ProgrammaticDspSupplyPathView(
        Long id,
        Long tenantId,
        Long lineItemId,
        String exchangeKey,
        String dealId,
        String sellerId,
        String sellerDomain,
        String inventoryType,
        String adsTxtStatus,
        String sellersJsonStatus,
        boolean schainComplete,
        String status,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
