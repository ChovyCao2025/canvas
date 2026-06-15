package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiEmbedTicketCommand(
        String resourceType,
        String resourceKey,
        String scope,
        Map<String, String> filters,
        Integer ttlSeconds,
        List<String> allowedDomains,
        Map<String, String> parameters,
        Integer maxAccessCount,
        Integer rateLimitPerMinute) {

    public BiEmbedTicketCommand {
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        allowedDomains = allowedDomains == null ? List.of() : List.copyOf(allowedDomains);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
