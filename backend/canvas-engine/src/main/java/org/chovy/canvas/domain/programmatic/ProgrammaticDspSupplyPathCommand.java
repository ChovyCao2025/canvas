package org.chovy.canvas.domain.programmatic;

import java.util.Map;

/**
 * ProgrammaticDspSupplyPathCommand 承载 domain.programmatic 场景中的不可变数据快照。
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
 */
public record ProgrammaticDspSupplyPathCommand(
        Long lineItemId,
        String exchangeKey,
        String dealId,
        String sellerId,
        String sellerDomain,
        String inventoryType,
        String adsTxtStatus,
        String sellersJsonStatus,
        Boolean schainComplete,
        String status,
        Map<String, Object> metadata) {
}
