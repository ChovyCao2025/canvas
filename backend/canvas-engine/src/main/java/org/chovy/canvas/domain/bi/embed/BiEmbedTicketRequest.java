package org.chovy.canvas.domain.bi.embed;

import java.util.Map;
import java.util.List;

/**
 * BiEmbedTicketRequest 承载 domain.bi.embed 场景中的不可变数据快照。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param scope scope 字段。
 * @param filters filters 字段。
 * @param ttlSeconds ttlSeconds 字段。
 * @param allowedDomains allowedDomains 字段。
 * @param parameters parameters 字段。
 * @param maxAccessCount maxAccessCount 字段。
 * @param rateLimitPerMinute rateLimitPerMinute 字段。
 */
public record BiEmbedTicketRequest(
        String resourceType,
        String resourceKey,
        String scope,
        Map<String, String> filters,
        Integer ttlSeconds,
        List<String> allowedDomains,
        Map<String, String> parameters,
        Integer maxAccessCount,
        Integer rateLimitPerMinute
) {
    public BiEmbedTicketRequest {
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        allowedDomains = allowedDomains == null ? List.of() : List.copyOf(allowedDomains);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    /**
     * 创建 BiEmbedTicketRequest 实例并注入 domain.bi.embed 场景依赖。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @param scope scope 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     * @param filters filters 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     * @param ttlSeconds ttl seconds 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     */
    public BiEmbedTicketRequest(String resourceType,
                                String resourceKey,
                                String scope,
                                Map<String, String> filters,
                                Integer ttlSeconds) {
        this(resourceType, resourceKey, scope, filters, ttlSeconds, List.of(), Map.of(), null, null);
    }

    /**
     * 创建 BiEmbedTicketRequest 实例并注入 domain.bi.embed 场景依赖。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @param scope scope 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     * @param filters filters 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     * @param ttlSeconds ttl seconds 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     * @param allowedDomains allowed domains 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     */
    public BiEmbedTicketRequest(String resourceType,
                                String resourceKey,
                                String scope,
                                Map<String, String> filters,
                                Integer ttlSeconds,
                                List<String> allowedDomains) {
        this(resourceType, resourceKey, scope, filters, ttlSeconds, allowedDomains, Map.of(), null, null);
    }

    /**
     * 创建 BiEmbedTicketRequest 实例并注入 domain.bi.embed 场景依赖。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @param scope scope 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     * @param filters filters 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     * @param ttlSeconds ttl seconds 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     * @param allowedDomains allowed domains 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     * @param parameters parameters 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     */
    public BiEmbedTicketRequest(String resourceType,
                                String resourceKey,
                                String scope,
                                Map<String, String> filters,
                                Integer ttlSeconds,
                                List<String> allowedDomains,
                                Map<String, String> parameters) {
        this(resourceType, resourceKey, scope, filters, ttlSeconds, allowedDomains, parameters, null, null);
    }

    /**
     * 创建 BiEmbedTicketRequest 实例并注入 domain.bi.embed 场景依赖。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @param scope scope 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     * @param filters filters 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     * @param ttlSeconds ttl seconds 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     * @param allowedDomains allowed domains 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     * @param maxAccessCount max access count 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     * @param rateLimitPerMinute rate limit per minute 参数，用于 BiEmbedTicketRequest 流程中的校验、计算或对象转换。
     */
    public BiEmbedTicketRequest(String resourceType,
                                String resourceKey,
                                String scope,
                                Map<String, String> filters,
                                Integer ttlSeconds,
                                List<String> allowedDomains,
                                Integer maxAccessCount,
                                Integer rateLimitPerMinute) {
        this(resourceType, resourceKey, scope, filters, ttlSeconds, allowedDomains, Map.of(),
                maxAccessCount, rateLimitPerMinute);
    }
}
