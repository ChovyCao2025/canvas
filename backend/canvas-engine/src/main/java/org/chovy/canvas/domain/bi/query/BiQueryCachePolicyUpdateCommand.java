package org.chovy.canvas.domain.bi.query;

import java.util.List;

public record BiQueryCachePolicyUpdateCommand(
        Boolean defaultEnabled,
        Long defaultTtlSeconds,
        String defaultCacheMode,
        List<ResourcePolicyCommand> resources) {

    public BiQueryCachePolicyUpdateCommand {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }

    public record ResourcePolicyCommand(
            String resourceType,
            String resourceKey,
            Boolean enabled,
            Long ttlSeconds,
            String cacheMode) {
    }
}
