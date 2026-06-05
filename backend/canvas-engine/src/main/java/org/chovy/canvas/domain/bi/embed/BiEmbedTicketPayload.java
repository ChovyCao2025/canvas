package org.chovy.canvas.domain.bi.embed;

import java.time.Instant;
import java.util.Map;

public record BiEmbedTicketPayload(
        Long tenantId,
        String username,
        String resourceType,
        String resourceKey,
        String scope,
        Map<String, String> filters,
        String nonce,
        Instant issuedAt,
        Instant expiresAt
) {
    public BiEmbedTicketPayload {
        filters = filters == null ? Map.of() : Map.copyOf(filters);
    }
}
