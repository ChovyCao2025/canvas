package org.chovy.canvas.domain.search;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SearchMarketingSyncCommand 承载 domain.search 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param sourceId sourceId 字段。
 * @param provider provider 字段。
 * @param externalAccountId externalAccountId 字段。
 * @param runType runType 字段。
 * @param windowStart windowStart 字段。
 * @param windowEnd windowEnd 字段。
 * @param cursorValue cursorValue 字段。
 * @param metadata metadata 字段。
 */
public record SearchMarketingSyncCommand(
        Long tenantId,
        Long sourceId,
        String provider,
        String externalAccountId,
        String runType,
        LocalDate windowStart,
        LocalDate windowEnd,
        String cursorValue,
        Map<String, Object> metadata) {

    public SearchMarketingSyncCommand {
        if (metadata == null || metadata.isEmpty()) {
            metadata = Map.of();
        } else {
            Map<String, Object> normalizedMetadata = new LinkedHashMap<>();
            metadata.forEach((key, value) -> {
                if (key != null && value != null) {
                    normalizedMetadata.put(key, value);
                }
            });
            metadata = Map.copyOf(normalizedMetadata);
        }
    }
}
