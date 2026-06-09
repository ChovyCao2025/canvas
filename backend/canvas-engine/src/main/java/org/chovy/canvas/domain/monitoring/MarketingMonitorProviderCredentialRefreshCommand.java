package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorProviderCredentialRefreshCommand 承载 domain.monitoring 场景中的不可变数据快照。
 * @param rotateRefreshToken rotateRefreshToken 字段。
 */
public record MarketingMonitorProviderCredentialRefreshCommand(Boolean rotateRefreshToken) {
}
