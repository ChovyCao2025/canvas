package org.chovy.canvas.domain.bi.embed;

import java.util.Map;

public record BiEmbedTicketRequest(
        String resourceType,
        String resourceKey,
        String scope,
        Map<String, String> filters,
        Integer ttlSeconds
) {
    public BiEmbedTicketRequest {
        filters = filters == null ? Map.of() : Map.copyOf(filters);
    }
}
