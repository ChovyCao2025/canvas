package org.chovy.canvas.domain.bi.embed;

import java.util.Map;
import java.util.List;

public record BiEmbedTicketRequest(
        String resourceType,
        String resourceKey,
        String scope,
        Map<String, String> filters,
        Integer ttlSeconds,
        List<String> allowedDomains,
        Map<String, String> parameters,
        Integer maxAccessCount,
        Integer rateLimitPerMinute
) {
    public BiEmbedTicketRequest {
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        allowedDomains = allowedDomains == null ? List.of() : List.copyOf(allowedDomains);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    public BiEmbedTicketRequest(String resourceType,
                                String resourceKey,
                                String scope,
                                Map<String, String> filters,
                                Integer ttlSeconds) {
        this(resourceType, resourceKey, scope, filters, ttlSeconds, List.of(), Map.of(), null, null);
    }

    public BiEmbedTicketRequest(String resourceType,
                                String resourceKey,
                                String scope,
                                Map<String, String> filters,
                                Integer ttlSeconds,
                                List<String> allowedDomains) {
        this(resourceType, resourceKey, scope, filters, ttlSeconds, allowedDomains, Map.of(), null, null);
    }

    public BiEmbedTicketRequest(String resourceType,
                                String resourceKey,
                                String scope,
                                Map<String, String> filters,
                                Integer ttlSeconds,
                                List<String> allowedDomains,
                                Map<String, String> parameters) {
        this(resourceType, resourceKey, scope, filters, ttlSeconds, allowedDomains, parameters, null, null);
    }

    public BiEmbedTicketRequest(String resourceType,
                                String resourceKey,
                                String scope,
                                Map<String, String> filters,
                                Integer ttlSeconds,
                                List<String> allowedDomains,
                                Integer maxAccessCount,
                                Integer rateLimitPerMinute) {
        this(resourceType, resourceKey, scope, filters, ttlSeconds, allowedDomains, Map.of(),
                maxAccessCount, rateLimitPerMinute);
    }
}
