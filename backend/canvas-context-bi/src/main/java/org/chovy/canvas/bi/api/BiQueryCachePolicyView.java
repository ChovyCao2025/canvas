package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiQueryCachePolicyView 视图。
 */
public record BiQueryCachePolicyView(
        /**
         * defaultEnabled 字段值。
         */
        boolean defaultEnabled,
        /**
         * defaultTtlSeconds 对应的数据集合。
         */
        long defaultTtlSeconds,
        /**
         * defaultCacheMode 字段值。
         */
        String defaultCacheMode,
        List<Map<String, Object>> resources) {

    public BiQueryCachePolicyView {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }
}
