package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorProviderOAuthAuthorizationQuery 承载 domain.monitoring 场景中的不可变数据快照。
 * @param credentialKey credentialKey 字段。
 * @param providerType providerType 字段。
 * @param status status 字段。
 * @param limit limit 字段。
 */
public record MarketingMonitorProviderOAuthAuthorizationQuery(
        String credentialKey,
        String providerType,
        String status,
        int limit) {
}
