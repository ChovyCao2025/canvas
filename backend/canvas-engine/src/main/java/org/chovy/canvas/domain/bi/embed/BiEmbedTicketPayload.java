package org.chovy.canvas.domain.bi.embed;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record BiEmbedTicketPayload(
        Long tenantId,
        String username,
        String resourceType,
        String resourceKey,
        String scope,
        Map<String, String> filters,
        Map<String, String> parameters,
        List<String> allowedDomains,
        Integer maxAccessCount,
        Integer rateLimitPerMinute,
        String nonce,
        Instant issuedAt,
        Instant expiresAt
) {
    public BiEmbedTicketPayload {
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        allowedDomains = allowedDomains == null ? List.of() : List.copyOf(allowedDomains);
    }
}
