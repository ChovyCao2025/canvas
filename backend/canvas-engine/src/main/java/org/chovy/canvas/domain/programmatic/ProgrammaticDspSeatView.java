package org.chovy.canvas.domain.programmatic;

import java.time.LocalDateTime;
import java.util.Map;

public record ProgrammaticDspSeatView(
        Long id,
        Long tenantId,
        String provider,
        String seatKey,
        String displayName,
        String advertiserAccountId,
        String currency,
        String timezone,
        String supplyChainEnforcement,
        boolean enabled,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
