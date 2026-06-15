package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiQueryCachePolicyView(
        boolean defaultEnabled,
        long defaultTtlSeconds,
        String defaultCacheMode,
        List<Map<String, Object>> resources) {

    public BiQueryCachePolicyView {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }
}
