package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiEmbedTicketCommand 命令。
 */
public record BiEmbedTicketCommand(
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源键。
         */
        String resourceKey,
        /**
         * scope 字段值。
         */
        String scope,
        /**
         * 筛选条件。
         */
        Map<String, String> filters,
        /**
         * ttlSeconds 对应的数据集合。
         */
        Integer ttlSeconds,
        /**
         * allowedDomains 对应的数据集合。
         */
        List<String> allowedDomains,
        /**
         * parameters 对应的数据集合。
         */
        Map<String, String> parameters,
        /**
         * maxAccessCount 对应的统计数量。
         */
        Integer maxAccessCount,
        Integer rateLimitPerMinute) {

    public BiEmbedTicketCommand {
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        allowedDomains = allowedDomains == null ? List.of() : List.copyOf(allowedDomains);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
