package org.chovy.canvas.domain.programmatic;

import java.util.Map;

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
