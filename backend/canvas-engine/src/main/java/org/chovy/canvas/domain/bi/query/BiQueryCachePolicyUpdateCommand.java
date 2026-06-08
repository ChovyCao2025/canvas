package org.chovy.canvas.domain.bi.query;

import java.util.List;

/**
 * BiQueryCachePolicyUpdateCommand 承载对应领域的业务规则、流程编排和结果转换。
 */
public record BiQueryCachePolicyUpdateCommand(
        Boolean defaultEnabled,
        Long defaultTtlSeconds,
        String defaultCacheMode,
        List<ResourcePolicyCommand> resources) {

    public BiQueryCachePolicyUpdateCommand {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }

    /**
     * ResourcePolicyCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ResourcePolicyCommand(
            String resourceType,
            String resourceKey,
            Boolean enabled,
            Long ttlSeconds,
            String cacheMode) {
    }
}
