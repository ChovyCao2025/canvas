package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiQueryCachePolicyCommand 命令。
 */
public record BiQueryCachePolicyCommand(
        /**
         * defaultEnabled 字段值。
         */
        Boolean defaultEnabled,
        /**
         * defaultTtlSeconds 对应的数据集合。
         */
        Long defaultTtlSeconds,
        /**
         * defaultCacheMode 字段值。
         */
        String defaultCacheMode,
        List<Map<String, Object>> resources) {

    public BiQueryCachePolicyCommand {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }
}
