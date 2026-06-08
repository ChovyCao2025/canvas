package org.chovy.canvas.domain.bi.query;

import java.util.List;

/**
 * BiQueryCachePolicyView 承载对应领域的业务规则、流程编排和结果转换。
 */
public record BiQueryCachePolicyView(
        boolean defaultEnabled,
        long defaultTtlSeconds,
        String defaultCacheMode,
        List<ResourcePolicyView> resources) {

    public BiQueryCachePolicyView {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }

    /**
     * ResourcePolicyView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ResourcePolicyView(
            String resourceType,
            String resourceKey,
            boolean enabled,
            long ttlSeconds,
            String cacheMode) {
    }
}
