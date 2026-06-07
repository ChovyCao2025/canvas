package org.chovy.canvas.domain.programmatic;

import java.time.LocalDateTime;
import java.util.Map;

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
