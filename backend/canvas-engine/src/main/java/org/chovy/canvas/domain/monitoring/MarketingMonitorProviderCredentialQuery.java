package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorProviderCredentialQuery 承载 domain.monitoring 场景中的不可变数据快照。
 * @param providerType providerType 字段。
 * @param authType authType 字段。
 * @param status status 字段。
 * @param limit limit 字段。
 */
public record MarketingMonitorProviderCredentialQuery(
        String providerType,
        String authType,
        String status,
        int limit) {
}
