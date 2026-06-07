package org.chovy.canvas.domain.bi.query;

import java.util.List;

public record BiQueryCachePolicyView(
        boolean defaultEnabled,
        long defaultTtlSeconds,
        String defaultCacheMode,
        List<ResourcePolicyView> resources) {

    public BiQueryCachePolicyView {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }

    public record ResourcePolicyView(
            String resourceType,
            String resourceKey,
            boolean enabled,
            long ttlSeconds,
            String cacheMode) {
    }
}
