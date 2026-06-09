package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorProviderCredentialDueRefreshCommand 承载 domain.monitoring 场景中的不可变数据快照。
 * @param windowMinutes windowMinutes 字段。
 * @param limit limit 字段。
 */
public record MarketingMonitorProviderCredentialDueRefreshCommand(
        Integer windowMinutes,
        Integer limit) {
}
