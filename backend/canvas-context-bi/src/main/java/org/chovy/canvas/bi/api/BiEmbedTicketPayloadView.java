package org.chovy.canvas.bi.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
/**
 * BiEmbedTicketPayloadView 视图。
 */
public record BiEmbedTicketPayloadView(
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * username 字段值。
         */
        String username,
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
         * parameters 对应的数据集合。
         */
        Map<String, String> parameters,
        /**
         * allowedDomains 对应的数据集合。
         */
        List<String> allowedDomains,
        /**
         * maxAccessCount 对应的统计数量。
         */
        Integer maxAccessCount,
        /**
         * rateLimitPerMinute 字段值。
         */
        Integer rateLimitPerMinute,
        /**
         * nonce 字段值。
         */
        String nonce,
        /**
         * issuedAt 对应的时间。
         */
        Instant issuedAt,
        Instant expiresAt) {

    public BiEmbedTicketPayloadView {
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        allowedDomains = allowedDomains == null ? List.of() : List.copyOf(allowedDomains);
    }
}
