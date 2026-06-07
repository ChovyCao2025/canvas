package org.chovy.canvas.domain.search;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

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
