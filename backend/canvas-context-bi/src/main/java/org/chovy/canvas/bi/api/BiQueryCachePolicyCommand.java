package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiQueryCachePolicyCommand(
        Boolean defaultEnabled,
        Long defaultTtlSeconds,
        String defaultCacheMode,
        List<Map<String, Object>> resources) {

    public BiQueryCachePolicyCommand {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }
}
