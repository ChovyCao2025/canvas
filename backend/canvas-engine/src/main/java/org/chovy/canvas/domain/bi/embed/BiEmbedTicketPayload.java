package org.chovy.canvas.domain.bi.embed;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * BiEmbedTicketPayload 承载 domain.bi.embed 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param username username 字段。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param scope scope 字段。
 * @param filters filters 字段。
 * @param parameters parameters 字段。
 * @param allowedDomains allowedDomains 字段。
 * @param maxAccessCount maxAccessCount 字段。
 * @param rateLimitPerMinute rateLimitPerMinute 字段。
 * @param nonce nonce 字段。
 * @param issuedAt issuedAt 字段。
 * @param expiresAt expiresAt 字段。
 */
public record BiEmbedTicketPayload(
        Long tenantId,
        String username,
        String resourceType,
        String resourceKey,
        String scope,
        Map<String, String> filters,
        Map<String, String> parameters,
        List<String> allowedDomains,
        Integer maxAccessCount,
        Integer rateLimitPerMinute,
        String nonce,
        Instant issuedAt,
        Instant expiresAt
) {
    public BiEmbedTicketPayload {
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        allowedDomains = allowedDomains == null ? List.of() : List.copyOf(allowedDomains);
    }
}
