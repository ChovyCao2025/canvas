package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorProviderCredentialEventQuery 承载 domain.monitoring 场景中的不可变数据快照。
 * @param credentialKey credentialKey 字段。
 * @param eventType eventType 字段。
 * @param status status 字段。
 * @param limit limit 字段。
 */
public record MarketingMonitorProviderCredentialEventQuery(
        String credentialKey,
        String eventType,
        String status,
        int limit) {
}
